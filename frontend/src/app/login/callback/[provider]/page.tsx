"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { api, ApiError } from "@/lib/api";
import { saveToken } from "@/lib/auth";
import { resolveReportNavigationPath } from "@/lib/reportProgress";

export default function SocialLoginCallbackPage() {
  const router = useRouter();
  const params = useParams<{ provider: string }>();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const provider = params.provider;
    const code = searchParams.get("code");
    const state = searchParams.get("state");

    if (!code) {
      setError("로그인 코드가 없어요. 다시 시도해 주세요.");
      return;
    }

    api
      .socialLoginCallback(provider, code, state)
      .then(async (login) => {
        saveToken(login.accessToken);

        if (login.isNewUser || !login.agreementCompleted) {
          router.replace("/onboarding/life-event");
          return;
        }

        const target = await api.getLatestReportRoute();
        router.replace(resolveReportNavigationPath(target));
      })
      .catch((e) => {
        const msg =
          e instanceof ApiError
            ? `로그인에 실패했어요. (${e.message})`
            : "로그인 중 문제가 발생했어요.";
        setError(msg);
      });
  }, [params.provider, router, searchParams]);

  return (
    <AppShell>
      <div className="center-state">
        {error ? (
          <>
            <p>{error}</p>
            <button type="button" className="btn" onClick={() => router.replace("/login")}>
              로그인으로 돌아가기
            </button>
          </>
        ) : (
          "로그인 처리 중…"
        )}
      </div>
    </AppShell>
  );
}
