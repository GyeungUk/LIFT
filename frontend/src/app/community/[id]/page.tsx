"use client";

import Link from "next/link";
import { use, useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { AuthGuard } from "@/components/AuthGuard";
import { ArrowLeftIcon, ChatIcon, HeartIcon, TrashIcon } from "@/components/Icons";
import { api, ApiError } from "@/lib/api";
import { eventTypeLabel } from "@/lib/labels";
import type { CommunityCategory, CommunityComment, CommunityPostDetail } from "@/lib/types";

const CATEGORY_TAG_CLASS: Record<CommunityCategory, string> = {
  JOB_CHANGE: "job-change",
  RETIREMENT: "retirement",
  UNEMPLOYMENT: "unemployment",
};

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function CommentItem({
  comment,
  onDelete,
}: {
  comment: CommunityComment;
  onDelete: (comment: CommunityComment) => void;
}) {
  return (
    <li className="comment-card">
      <div className="comment-head">
        <span className="comment-avatar">{comment.authorName.slice(0, 1)}</span>
        <span className="comment-author">{comment.authorName}</span>
        <span className="comment-time">{formatDate(comment.createdAt)}</span>
        {comment.mine && (
          <button
            type="button"
            className="comment-delete"
            onClick={() => onDelete(comment)}
            aria-label="댓글 삭제"
          >
            <TrashIcon />
          </button>
        )}
      </div>
      <p className="comment-text">{comment.content}</p>
    </li>
  );
}

function CommunityDetailInner({ postId }: { postId: number }) {
  const router = useRouter();
  const [post, setPost] = useState<CommunityPostDetail | null>(null);
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(null);
    api
      .getCommunityPost(postId)
      .then(setPost)
      .catch((e) =>
        setError(e instanceof ApiError ? e.message : "커뮤니티 글을 불러오지 못했어요."),
      );
  }, [postId]);

  async function handleLike() {
    if (!post) return;
    try {
      const result = post.liked
        ? await api.unlikeCommunityPost(post.postId)
        : await api.likeCommunityPost(post.postId);
      setPost({ ...post, liked: result.liked, likeCount: result.likeCount });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "좋아요 처리에 실패했어요.");
    }
  }

  async function handleDeletePost() {
    if (!post || !window.confirm("이 글을 삭제할까요?")) return;
    try {
      await api.deleteCommunityPost(post.postId);
      router.replace("/community");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "글 삭제에 실패했어요.");
    }
  }

  async function handleCreateComment(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const content = comment.trim();
    if (!post || !content) return;

    setSubmitting(true);
    setError(null);
    try {
      const created = await api.createCommunityComment(post.postId, { content });
      setPost({
        ...post,
        commentCount: post.commentCount + 1,
        comments: [...post.comments, created],
      });
      setComment("");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "댓글 작성에 실패했어요.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteComment(target: CommunityComment) {
    if (!post || !window.confirm("이 댓글을 삭제할까요?")) return;
    try {
      await api.deleteCommunityComment(post.postId, target.commentId);
      setPost({
        ...post,
        commentCount: Math.max(0, post.commentCount - 1),
        comments: post.comments.filter((item) => item.commentId !== target.commentId),
      });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "댓글 삭제에 실패했어요.");
    }
  }

  if (error && !post) return <div className="error-box">{error}</div>;
  if (!post) return <div className="center-state">글을 불러오는 중…</div>;

  return (
    <main className="content-narrow section-page">
      <Link href="/community" className="detail-back">
        <ArrowLeftIcon /> 커뮤니티로 돌아가기
      </Link>

      <article className="detail-card">
        <div className="post-top">
          <span className={`post-tag ${CATEGORY_TAG_CLASS[post.category]}`}>
            {eventTypeLabel[post.category]}
          </span>
          <span className="post-time">{post.authorName}</span>
          <span className="post-time">{formatDate(post.createdAt)}</span>
        </div>
        <h1>{post.title}</h1>
        <p className="detail-body">{post.content}</p>

        <div className="detail-divider" />

        <div className="detail-actions">
          <button
            type="button"
            className={`like-big ${post.liked ? "liked" : ""}`}
            onClick={handleLike}
          >
            <HeartIcon filled={post.liked} /> 좋아요 {post.likeCount}
          </button>
          <span className="pill-btn">
            <ChatIcon /> {post.commentCount}
          </span>
          {post.mine && (
            <button type="button" className="pill-btn danger" onClick={handleDeletePost}>
              <TrashIcon /> 삭제
            </button>
          )}
        </div>
      </article>

      {error && <div className="error-box">{error}</div>}

      <section className="comment-section">
        <h2>
          댓글 <b>{post.commentCount}</b>
        </h2>

        <form className="comment-form" onSubmit={handleCreateComment}>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={1000}
            rows={3}
            placeholder="익명 댓글을 남겨보세요."
            aria-label="댓글 내용"
          />
          <div className="comment-form-foot">
            <span className="char-count">{comment.length.toLocaleString()} / 1,000</span>
            <button type="submit" className="btn" disabled={submitting || !comment.trim()}>
              {submitting ? "등록 중…" : "댓글 등록"}
            </button>
          </div>
        </form>

        {post.comments.length === 0 ? (
          <div className="empty-panel">
            <span>아직 댓글이 없어요. 첫 댓글을 남겨보세요.</span>
          </div>
        ) : (
          <ul className="comment-list">
            {post.comments.map((item) => (
              <CommentItem key={item.commentId} comment={item} onDelete={handleDeleteComment} />
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}

export default function CommunityDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  return (
    <AppShell showLogout wide>
      <AuthGuard>
        <CommunityDetailInner postId={Number(id)} />
      </AuthGuard>
    </AppShell>
  );
}
