package com.bodeum.domain.lifetransition.service;

import com.bodeum.domain.lifetransition.dto.response.BenefitEstimateResDTO;
import com.bodeum.domain.lifetransition.dto.response.BenefitSummaryResDTO;
import com.bodeum.domain.lifetransition.enumtype.BenefitEstimateKind;
import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import com.bodeum.domain.lifetransition.model.LifeAssessment;
import com.bodeum.domain.lifetransition.model.LifeReport;
import com.bodeum.domain.lifetransition.model.ReportItem;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 리포트 각 절차의 "예상 수령/절감 금액"을 계산한다.
 *
 * <p>모든 금액은 <b>예상치</b>이며, 실제 금액은 개인 상황과 기관 판단에 따라 달라진다.
 * 산정에 쓰는 상수(상·하한액, 요율 등)는 {@code 2025년 기준}이며 아래 상수만 갱신하면 된다.
 * 실제 계산은 이 서비스에만 있으므로, 정책 변경 시 이 파일만 수정한다.
 */
@Service
public class BenefitEstimationService {

    static final String BASIS_YEAR = "2026";

    // --- 실업급여(구직급여) 기준 (2026년, 1/1 퇴사자부터 적용) ---
    private static final long UNEMPLOYMENT_DAILY_UPPER = 68_100L;   // 1일 상한액(7년 만에 인상)
    private static final long UNEMPLOYMENT_DAILY_LOWER = 66_048L;   // 1일 하한액(최저임금 10,320 × 80% × 8h)
    private static final double WAGE_REPLACEMENT_RATE = 0.60;       // 평균임금 대비 지급률

    // --- 국민연금 (2026년) ---
    private static final double PENSION_RATE = 0.09;                // 연금 보험료율
    private static final long PENSION_MONTHLY_INCOME_CAP = 6_370_000L; // 기준소득월액 상한

    // --- 건강보험 (2026년, 요율 동결) ---
    private static final double HEALTH_EMPLOYEE_RATE = 0.03545;     // 직장가입자 본인부담 보험료율(7.09%/2)

    // 1일 평균임금 근사: 월급 / 30
    private static final double DAYS_PER_MONTH = 30.0;

    private static final String BASIS_NOTE =
            BASIS_YEAR + "년 기준으로 계산한 예상치예요. 실제 금액은 개인 상황과 관할 기관 판단에 따라 달라질 수 있어요.";

    /**
     * 리포트 전체를 계산해 절차별 예상 금액 + 합계 요약을 반환한다.
     */
    public ReportEstimation estimate(LifeReport report) {
        return estimate(report, report.getAssessment().getMonthlyAverageWage());
    }

    /**
     * PDF 저장 시점에 입력받은 월급으로만 임시 계산한다. 입력값은 저장하지 않는다.
     */
    public ReportEstimation estimateWithMonthlyWage(LifeReport report, Integer monthlyAverageWage) {
        return estimate(report, monthlyAverageWage);
    }

    /**
     * 월급 입력 없이 만드는 PDF 버전. 진단에 저장된 월급이 있어도 범위/산식 중심으로 표시한다.
     */
    public ReportEstimation estimateWithoutMonthlyWage(LifeReport report) {
        return estimate(report, null);
    }

    private ReportEstimation estimate(LifeReport report, Integer wage) {
        LifeAssessment a = report.getAssessment();

        Map<ProcedureType, BenefitEstimateResDTO> perItem = new EnumMap<>(ProcedureType.class);
        long totalReceive = 0L;
        long totalMonthlySaving = 0L;
        int receiveCount = 0;
        boolean hasVariable = false;
        boolean estimated = false;

        for (ReportItem item : report.getItems()) {
            BenefitEstimateResDTO est = estimateItem(item, a, wage);
            perItem.put(item.getProcedureType(), est);

            switch (est.kind()) {
                case RECEIVE -> {
                    if (est.amount() != null) {
                        totalReceive += est.amount();
                        receiveCount++;
                        estimated = true;
                    }
                }
                case SAVE_MONTHLY -> {
                    if (est.amount() != null) {
                        totalMonthlySaving += est.amount();
                        estimated = true;
                    }
                }
                case VARIABLE -> hasVariable = true;
                case NOT_ESTIMATED -> { /* 합계에 반영하지 않음 */ }
            }
        }

        BenefitSummaryResDTO summary = new BenefitSummaryResDTO(
                totalReceive,
                totalMonthlySaving,
                receiveCount,
                hasVariable,
                estimated,
                BASIS_NOTE
        );
        return new ReportEstimation(summary, perItem);
    }

    private BenefitEstimateResDTO estimateItem(ReportItem item, LifeAssessment a, Integer wage) {
        return switch (item.getProcedureType()) {
            case UNEMPLOYMENT_BENEFIT -> estimateUnemployment(item, a, wage);
            case SEVERANCE_PAY -> estimateSeverance(a, wage);
            case NATIONAL_PENSION_EXCEPTION -> estimatePensionException(wage);
            case HEALTH_INSURANCE_CONTINUATION -> estimateHealthInsurance(wage);
            case TAX_CHECK -> variable(
                    "연말정산·종합소득세를 정산하면 환급을 받을 수 있어요",
                    "환급 여부·금액은 원천징수 내역과 공제 항목에 따라 달라져요.");
        };
    }

    /** 실업급여(구직급여) 총 예상 수급액 = 구직급여일액 × 소정급여일수. */
    private BenefitEstimateResDTO estimateUnemployment(ReportItem item, LifeAssessment a, Integer wage) {
        if (item.getEligibilityLevel() == EligibilityLevel.LOW) {
            return new BenefitEstimateResDTO(
                    BenefitEstimateKind.NOT_ESTIMATED, null, null,
                    "현재 조건으로는 수급이 어려울 수 있어요",
                    "고용보험 가입기간이 부족해 예상 금액을 계산하지 않았어요.");
        }
        if (wage == null || wage <= 0) {
            return notEstimatedByWage();
        }

        long dailyBenefit = clampDailyBenefit(wage / DAYS_PER_MONTH * WAGE_REPLACEMENT_RATE);
        int days = paymentDays(a.getEmploymentInsuranceMonths(), a.getAge());
        long total = dailyBenefit * days;

        String detail = String.format("1일 %s원 × %d일", comma(dailyBenefit), days);
        return new BenefitEstimateResDTO(
                BenefitEstimateKind.RECEIVE,
                total,
                "약 " + won(total),
                "실업급여로 약 " + won(total) + "을 받을 수 있어요",
                detail);
    }

    /** 퇴직금 ≈ 1일 평균임금 × 30 × 근속연수 ≈ 월급 × 근속연수. */
    private BenefitEstimateResDTO estimateSeverance(LifeAssessment a, Integer wage) {
        Integer tenure = a.getTenureYears();
        if (wage == null || wage <= 0 || tenure == null) {
            return notEstimatedByWage();
        }
        if (tenure < 1) {
            return new BenefitEstimateResDTO(
                    BenefitEstimateKind.NOT_ESTIMATED, null, null,
                    "근속 1년 미만은 퇴직금 대상이 아닐 수 있어요",
                    "근속연수가 1년 이상일 때 법정 퇴직금이 발생해요.");
        }

        long dailyAvg = Math.round(wage / DAYS_PER_MONTH);
        long total = dailyAvg * 30 * tenure;
        return new BenefitEstimateResDTO(
                BenefitEstimateKind.RECEIVE,
                total,
                "약 " + won(total),
                "퇴직금으로 약 " + won(total) + "을 받을 수 있어요",
                String.format("1일 평균임금 %s원 × 30일 × %d년", comma(dailyAvg), tenure));
    }

    /** 국민연금 납부예외 시 매달 유예되는 보험료 ≈ 월급 × 9%(상한 적용). */
    private BenefitEstimateResDTO estimatePensionException(Integer wage) {
        if (wage == null || wage <= 0) {
            return notEstimatedByWage();
        }
        long base = Math.min(wage, PENSION_MONTHLY_INCOME_CAP);
        long monthly = Math.round(base * PENSION_RATE);
        return new BenefitEstimateResDTO(
                BenefitEstimateKind.SAVE_MONTHLY,
                monthly,
                "월 약 " + won(monthly),
                "국민연금 납부예외로 매달 약 " + won(monthly) + "을 아낄 수 있어요",
                "소득 없는 기간 동안 납부를 유예해요. 다만 나중에 받을 연금액은 줄 수 있어요.");
    }

    /** 건강보험 임의계속 시 매달 내는 직장가입자 본인부담 보험료(예상). 지역가입자 전환 대비 절감 효과. */
    private BenefitEstimateResDTO estimateHealthInsurance(Integer wage) {
        if (wage == null || wage <= 0) {
            return notEstimatedByWage();
        }
        long monthly = Math.round(wage * HEALTH_EMPLOYEE_RATE);
        return new BenefitEstimateResDTO(
                BenefitEstimateKind.SAVE_MONTHLY,
                monthly,
                "월 약 " + won(monthly),
                "임의계속가입으로 보험료를 월 약 " + won(monthly) + " 수준으로 유지할 수 있어요",
                "퇴직 후 지역가입자로 바뀌면 재산·소득에 따라 보험료가 더 오를 수 있어, 그 차액만큼 절감돼요.");
    }

    private BenefitEstimateResDTO variable(String headline, String detail) {
        return new BenefitEstimateResDTO(BenefitEstimateKind.VARIABLE, null, null, headline, detail);
    }

    private BenefitEstimateResDTO notEstimatedByWage() {
        return new BenefitEstimateResDTO(
                BenefitEstimateKind.NOT_ESTIMATED, null, null,
                "급여 정보를 입력하면 예상 금액을 계산해 드려요",
                "PDF 저장 시 월 평균임금을 입력하면 금액이 표시돼요.");
    }

    private long clampDailyBenefit(double raw) {
        long v = Math.round(raw);
        if (v > UNEMPLOYMENT_DAILY_UPPER) return UNEMPLOYMENT_DAILY_UPPER;
        if (v < UNEMPLOYMENT_DAILY_LOWER) return UNEMPLOYMENT_DAILY_LOWER;
        return v;
    }

    /** 소정급여일수: 고용보험 가입기간 + 연령(50세 이상 우대). 2019.10 이후 기준. */
    private int paymentDays(Integer insuranceMonths, Integer age) {
        int years = (insuranceMonths == null ? 0 : insuranceMonths) / 12;
        boolean senior = age != null && age >= 50;
        if (years < 1) return 120;
        if (years < 3) return senior ? 180 : 150;
        if (years < 5) return senior ? 210 : 180;
        if (years < 10) return senior ? 240 : 210;
        return senior ? 270 : 240;
    }

    private static String comma(long v) {
        return String.format("%,d", v);
    }

    /** 원 단위 금액을 "1,200만원" / "1억 2,000만원" 처럼 읽기 쉽게 변환. */
    private static String won(long amount) {
        if (amount < 10_000) {
            return comma(amount) + "원";
        }
        long eok = amount / 100_000_000;
        long man = (amount % 100_000_000) / 10_000;
        StringBuilder sb = new StringBuilder();
        if (eok > 0) {
            sb.append(comma(eok)).append("억");
            if (man > 0) sb.append(" ");
        }
        if (man > 0 || eok == 0) {
            sb.append(comma(man)).append("만");
        }
        sb.append("원");
        return sb.toString();
    }

    /**
     * 리포트 예상 금액 계산 결과.
     */
    public record ReportEstimation(
            BenefitSummaryResDTO summary,
            Map<ProcedureType, BenefitEstimateResDTO> perItem
    ) {
    }
}
