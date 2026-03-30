# One-shot domain package migration (run from repo root: python scripts/migrate_domain_packages.py)
from __future__ import annotations

import re
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
JAVA = BASE / "src/main/java/com/example/studywithme"
TEST_JAVA = BASE / "src/test/java/com/example/studywithme"


def domain_for_entity_file(name: str) -> str:
    m = {
        "User.java": "user",
        "UserProfile.java": "user",
        "UserPreference.java": "user",
        "UserOnlineStatus.java": "user",
        "UserActivity.java": "user",
        "UserStudyStats.java": "user",
        "Post.java": "board",
        "PostLike.java": "board",
        "PostLikeId.java": "board",
        "PostApplication.java": "board",
        "Bookmark.java": "board",
        "Certification.java": "board",
        "Comment.java": "comment",
        "CommentLike.java": "comment",
        "CommentLikeId.java": "comment",
        "StudyGroup.java": "studygroup",
        "StudyGroupMember.java": "studygroup",
        "StudyGroupChat.java": "studygroup",
        "StudyGroupCalendar.java": "studygroup",
        "StudyGroupGoal.java": "studygroup",
        "StudyGroupResource.java": "studygroup",
        "StudyGroupSettings.java": "studygroup",
        "StudyGroupComment.java": "studygroup",
        "StudyGroupCurriculum.java": "studygroup",
        "StudyAttendance.java": "studygroup",
        "StudySession.java": "studysession",
        "StudyJournal.java": "studysession",
        "Notification.java": "notification",
        "FilterWord.java": "moderation",
        "FilterKeyword.java": "moderation",
        "FilterPattern.java": "moderation",
        "BlockedPost.java": "moderation",
        "BlockedComment.java": "moderation",
        "AILearningData.java": "moderation",
        "ChatMessage.java": "ai",
    }
    if name not in m:
        raise SystemExit(f"Unknown entity file: {name}")
    return m[name]


def domain_for_repo_file(name: str) -> str:
    stem = name.replace("Repository.java", "")
    # Map by prefix / known names
    user = {
        "User",
        "UserProfile",
        "UserPreference",
        "UserOnlineStatus",
        "UserActivity",
        "UserStudyStats",
    }
    board = {
        "Post",
        "PostLike",
        "PostApplication",
        "Bookmark",
        "Certification",
    }
    comment = {"Comment", "CommentLike"}
    studygroup = {
        "StudyGroup",
        "StudyGroupMember",
        "StudyGroupChat",
        "StudyGroupCalendar",
        "StudyGroupGoal",
        "StudyGroupResource",
        "StudyGroupSettings",
        "StudyGroupComment",
        "StudyGroupCurriculum",
        "StudyAttendance",
    }
    studysession = {"StudySession", "StudyJournal"}
    notification = {"Notification"}
    moderation = {
        "FilterWord",
        "FilterKeyword",
        "FilterPattern",
        "BlockedPost",
        "BlockedComment",
        "AILearningData",
    }
    ai = {"ChatMessage"}
    if stem in user:
        return "user"
    if stem in board:
        return "board"
    if stem in comment:
        return "comment"
    if stem in studygroup:
        return "studygroup"
    if stem in studysession:
        return "studysession"
    if stem in notification:
        return "notification"
    if stem in moderation:
        return "moderation"
    if stem in ai:
        return "ai"
    raise SystemExit(f"Unknown repository: {name}")


def domain_for_service_file(name: str) -> str:
    m = {
        "UserService.java": "user",
        "UserStatsService.java": "user",
        "UserActivityService.java": "user",
        "UserOnlineStatusService.java": "user",
        "UserRecommendationService.java": "user",
        "PostService.java": "board",
        "PostLikeService.java": "board",
        "PostApplicationService.java": "board",
        "BookmarkService.java": "board",
        "CommentService.java": "comment",
        "StudyGroupService.java": "studygroup",
        "StudyGroupManagementService.java": "studygroup",
        "StudyGroupChatService.java": "studygroup",
        "StudyGroupCalendarService.java": "studygroup",
        "StudyGroupGoalService.java": "studygroup",
        "StudyAttendanceService.java": "studygroup",
        "StudySessionService.java": "studysession",
        "StudyJournalService.java": "studysession",
        "NotificationService.java": "notification",
        "AdminService.java": "moderation",
        "ContentFilterService.java": "moderation",
        "AITagService.java": "ai",
        "AISummaryService.java": "ai",
        "ChatbotService.java": "ai",
        "PythonScriptExecutor.java": "ai",
        "PythonRecommendationService.java": "ai",
    }
    if name not in m:
        raise SystemExit(f"Unknown service file: {name}")
    return m[name]


def domain_for_controller_file(name: str) -> str | None:
    m = {
        "AdminController.java": "moderation",
        "ChatbotController.java": "ai",
        "CommentApiController.java": "comment",
        "NotificationApiController.java": "notification",
        "StudyGroupApiController.java": "studygroup",
        "StudyGroupCalendarApiController.java": "studygroup",
        "MainController.java": None,
    }
    if name not in m:
        raise SystemExit(f"Unknown controller file: {name}")
    return m[name]


def move_layer(folder: str, domain_fn, pkg_suffix: str):
    src_dir = JAVA / folder
    if not src_dir.is_dir():
        return
    for f in sorted(src_dir.glob("*.java")):
        dom = domain_fn(f.name)
        if dom is None:
            continue
        dest_dir = JAVA / dom / pkg_suffix
        dest_dir.mkdir(parents=True, exist_ok=True)
        text = f.read_text(encoding="utf-8")
        text = re.sub(
            rf"^package com\.example\.studywithme\.{re.escape(folder)};",
            f"package com.example.studywithme.{dom}.{pkg_suffix};",
            text,
            count=1,
            flags=re.MULTILINE,
        )
        (dest_dir / f.name).write_text(text, encoding="utf-8")
        f.unlink()


REPO_FILES = [
    "AILearningDataRepository.java",
    "BlockedCommentRepository.java",
    "BlockedPostRepository.java",
    "BookmarkRepository.java",
    "CertificationRepository.java",
    "ChatMessageRepository.java",
    "CommentLikeRepository.java",
    "CommentRepository.java",
    "FilterKeywordRepository.java",
    "FilterPatternRepository.java",
    "FilterWordRepository.java",
    "NotificationRepository.java",
    "PostApplicationRepository.java",
    "PostLikeRepository.java",
    "PostRepository.java",
    "StudyAttendanceRepository.java",
    "StudyGroupCalendarRepository.java",
    "StudyGroupChatRepository.java",
    "StudyGroupCommentRepository.java",
    "StudyGroupCurriculumRepository.java",
    "StudyGroupGoalRepository.java",
    "StudyGroupMemberRepository.java",
    "StudyGroupRepository.java",
    "StudyGroupResourceRepository.java",
    "StudyGroupSettingsRepository.java",
    "StudyJournalRepository.java",
    "StudySessionRepository.java",
    "UserActivityRepository.java",
    "UserOnlineStatusRepository.java",
    "UserPreferenceRepository.java",
    "UserProfileRepository.java",
    "UserRepository.java",
    "UserStudyStatsRepository.java",
]


def build_replacements() -> list[tuple[str, str]]:
    reps: list[tuple[str, str]] = []

    def add(domain: str, layer: str, simple: str):
        old = f"com.example.studywithme.{layer}.{simple}"
        new = f"com.example.studywithme.{domain}.{layer}.{simple}"
        reps.append((old, new))

    for fn, dom in ENTITY_DOMAIN.items():
        simple = fn.replace(".java", "")
        add(dom, "entity", simple)

    for fn in REPO_FILES:
        dom = domain_for_repo_file(fn)
        simple = fn.removesuffix(".java")
        add(dom, "repository", simple)

    for fn, dom in SERVICE_DOMAIN_MAP.items():
        simple = fn.replace(".java", "")
        add(dom, "service", simple)

    for fn, dom in CTRL_MAP.items():
        if dom is None:
            continue
        simple = fn.replace(".java", "")
        add(dom, "controller", simple)

    reps.sort(key=lambda x: len(x[0]), reverse=True)
    return reps


ENTITY_DOMAIN = {k: domain_for_entity_file(k) for k in [
    "User.java", "UserProfile.java", "UserPreference.java", "UserOnlineStatus.java",
    "UserActivity.java", "UserStudyStats.java", "Post.java", "PostLike.java", "PostLikeId.java",
    "PostApplication.java", "Bookmark.java", "Certification.java", "Comment.java", "CommentLike.java",
    "CommentLikeId.java", "StudyGroup.java", "StudyGroupMember.java", "StudyGroupChat.java",
    "StudyGroupCalendar.java", "StudyGroupGoal.java", "StudyGroupResource.java", "StudyGroupSettings.java",
    "StudyGroupComment.java", "StudyGroupCurriculum.java", "StudyAttendance.java", "StudySession.java",
    "StudyJournal.java", "Notification.java", "FilterWord.java", "FilterKeyword.java", "FilterPattern.java",
    "BlockedPost.java", "BlockedComment.java", "AILearningData.java", "ChatMessage.java",
]}

SERVICE_DOMAIN_MAP = {
    "UserService.java": "user",
    "UserStatsService.java": "user",
    "UserActivityService.java": "user",
    "UserOnlineStatusService.java": "user",
    "UserRecommendationService.java": "user",
    "PostService.java": "board",
    "PostLikeService.java": "board",
    "PostApplicationService.java": "board",
    "BookmarkService.java": "board",
    "CommentService.java": "comment",
    "StudyGroupService.java": "studygroup",
    "StudyGroupManagementService.java": "studygroup",
    "StudyGroupChatService.java": "studygroup",
    "StudyGroupCalendarService.java": "studygroup",
    "StudyGroupGoalService.java": "studygroup",
    "StudyAttendanceService.java": "studygroup",
    "StudySessionService.java": "studysession",
    "StudyJournalService.java": "studysession",
    "NotificationService.java": "notification",
    "AdminService.java": "moderation",
    "ContentFilterService.java": "moderation",
    "AITagService.java": "ai",
    "AISummaryService.java": "ai",
    "ChatbotService.java": "ai",
    "PythonScriptExecutor.java": "ai",
    "PythonRecommendationService.java": "ai",
}

CTRL_MAP = {
    "AdminController.java": "moderation",
    "ChatbotController.java": "ai",
    "CommentApiController.java": "comment",
    "NotificationApiController.java": "notification",
    "StudyGroupApiController.java": "studygroup",
    "StudyGroupCalendarApiController.java": "studygroup",
    "MainController.java": None,
}


def main():
    move_layer("entity", domain_for_entity_file, "entity")
    move_layer("repository", domain_for_repo_file, "repository")
    move_layer("service", lambda n: domain_for_service_file(n), "service")
    move_layer("controller", domain_for_controller_file, "controller")

    replacements = build_replacements()
    roots = [JAVA, TEST_JAVA]
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            orig = text
            for old, new in replacements:
                text = text.replace(old, new)
            if text != orig:
                path.write_text(text, encoding="utf-8")

    # Remove empty dirs
    for folder in ["entity", "repository", "service"]:
        d = JAVA / folder
        if d.exists() and not any(d.iterdir()):
            d.rmdir()

    print("Done. Moved packages and applied FQCN replacements under", JAVA)


if __name__ == "__main__":
    main()
