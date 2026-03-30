# Domain package migration (PowerShell). Run: powershell -ExecutionPolicy Bypass -File scripts/migrate_domain_packages.ps1
$ErrorActionPreference = "Stop"
$JAVA = Join-Path $PSScriptRoot "..\src\main\java\com\example\studywithme" | Resolve-Path

function Move-JavaLayer {
    param([string]$Folder, [hashtable]$NameToDomain, [string]$Suffix)
    $src = Join-Path $JAVA $Folder
    if (-not (Test-Path $src)) { return }
    Get-ChildItem $src -Filter "*.java" | ForEach-Object {
        $name = $_.Name
        if (-not $NameToDomain.ContainsKey($name)) { throw "Unknown ${Folder}: $name" }
        $dom = $NameToDomain[$name]
        if ($null -eq $dom) { return }
        $destDir = Join-Path $JAVA "$dom\$Suffix"
        New-Item -ItemType Directory -Force -Path $destDir | Out-Null
        $text = Get-Content $_.FullName -Raw -Encoding UTF8
        $text = $text -replace "^package com\.example\.studywithme\.$Folder;", "package com.example.studywithme.$dom.$Suffix;"
        $dest = Join-Path $destDir $name
        $utf8 = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($dest, $text, $utf8)
        Remove-Item $_.FullName
    }
}

$entityMap = @{
    "User.java"="user"; "UserProfile.java"="user"; "UserPreference.java"="user"; "UserOnlineStatus.java"="user"
    "UserActivity.java"="user"; "UserStudyStats.java"="user"
    "Post.java"="board"; "PostLike.java"="board"; "PostLikeId.java"="board"; "PostApplication.java"="board"
    "Bookmark.java"="board"; "Certification.java"="board"
    "Comment.java"="comment"; "CommentLike.java"="comment"; "CommentLikeId.java"="comment"
    "StudyGroup.java"="studygroup"; "StudyGroupMember.java"="studygroup"; "StudyGroupChat.java"="studygroup"
    "StudyGroupCalendar.java"="studygroup"; "StudyGroupGoal.java"="studygroup"; "StudyGroupResource.java"="studygroup"
    "StudyGroupSettings.java"="studygroup"; "StudyGroupComment.java"="studygroup"; "StudyGroupCurriculum.java"="studygroup"
    "StudyAttendance.java"="studygroup"
    "StudySession.java"="studysession"; "StudyJournal.java"="studysession"
    "Notification.java"="notification"
    "FilterWord.java"="moderation"; "FilterKeyword.java"="moderation"; "FilterPattern.java"="moderation"
    "BlockedPost.java"="moderation"; "BlockedComment.java"="moderation"; "AILearningData.java"="moderation"
    "ChatMessage.java"="ai"
}

$repoMap = @{
    "AILearningDataRepository.java"="moderation"; "BlockedCommentRepository.java"="moderation"; "BlockedPostRepository.java"="moderation"
    "BookmarkRepository.java"="board"; "CertificationRepository.java"="board"; "ChatMessageRepository.java"="ai"
    "CommentLikeRepository.java"="comment"; "CommentRepository.java"="comment"
    "FilterKeywordRepository.java"="moderation"; "FilterPatternRepository.java"="moderation"; "FilterWordRepository.java"="moderation"
    "NotificationRepository.java"="notification"; "PostApplicationRepository.java"="board"; "PostLikeRepository.java"="board"
    "PostRepository.java"="board"; "StudyAttendanceRepository.java"="studygroup"; "StudyGroupCalendarRepository.java"="studygroup"
    "StudyGroupChatRepository.java"="studygroup"; "StudyGroupCommentRepository.java"="studygroup"; "StudyGroupCurriculumRepository.java"="studygroup"
    "StudyGroupGoalRepository.java"="studygroup"; "StudyGroupMemberRepository.java"="studygroup"; "StudyGroupRepository.java"="studygroup"
    "StudyGroupResourceRepository.java"="studygroup"; "StudyGroupSettingsRepository.java"="studygroup"
    "StudyJournalRepository.java"="studysession"; "StudySessionRepository.java"="studysession"
    "UserActivityRepository.java"="user"; "UserOnlineStatusRepository.java"="user"; "UserPreferenceRepository.java"="user"
    "UserProfileRepository.java"="user"; "UserRepository.java"="user"; "UserStudyStatsRepository.java"="user"
}

$serviceMap = @{
    "UserService.java"="user"; "UserStatsService.java"="user"; "UserActivityService.java"="user"; "UserOnlineStatusService.java"="user"
    "UserRecommendationService.java"="user"
    "PostService.java"="board"; "PostLikeService.java"="board"; "PostApplicationService.java"="board"; "BookmarkService.java"="board"
    "CommentService.java"="comment"
    "StudyGroupService.java"="studygroup"; "StudyGroupManagementService.java"="studygroup"; "StudyGroupChatService.java"="studygroup"
    "StudyGroupCalendarService.java"="studygroup"; "StudyGroupGoalService.java"="studygroup"; "StudyAttendanceService.java"="studygroup"
    "StudySessionService.java"="studysession"; "StudyJournalService.java"="studysession"
    "NotificationService.java"="notification"
    "AdminService.java"="moderation"; "ContentFilterService.java"="moderation"
    "AITagService.java"="ai"; "AISummaryService.java"="ai"; "ChatbotService.java"="ai"
    "PythonScriptExecutor.java"="ai"; "PythonRecommendationService.java"="ai"
}

$ctrlMap = @{
    "AdminController.java"="moderation"; "ChatbotController.java"="ai"; "CommentApiController.java"="comment"
    "NotificationApiController.java"="notification"; "StudyGroupApiController.java"="studygroup"; "StudyGroupCalendarApiController.java"="studygroup"
    "MainController.java"=$null
}

Move-JavaLayer "entity" $entityMap "entity"
Move-JavaLayer "repository" $repoMap "repository"
Move-JavaLayer "service" $serviceMap "service"
Move-JavaLayer "controller" $ctrlMap "controller"

$replacements = @()
foreach ($kv in $entityMap.GetEnumerator()) {
    if ($null -eq $kv.Value) { continue }
    $cls = $kv.Key -replace '\.java$',''
    $replacements += ,@("com.example.studywithme.entity.$cls", "com.example.studywithme.$($kv.Value).entity.$cls")
}
foreach ($kv in $repoMap.GetEnumerator()) {
    $cls = $kv.Key -replace '\.java$',''
    $replacements += ,@("com.example.studywithme.repository.$cls", "com.example.studywithme.$($kv.Value).repository.$cls")
}
foreach ($kv in $serviceMap.GetEnumerator()) {
    $cls = $kv.Key -replace '\.java$',''
    $replacements += ,@("com.example.studywithme.service.$cls", "com.example.studywithme.$($kv.Value).service.$cls")
}
foreach ($kv in $ctrlMap.GetEnumerator()) {
    if ($null -eq $kv.Value) { continue }
    $cls = $kv.Key -replace '\.java$',''
    $replacements += ,@("com.example.studywithme.controller.$cls", "com.example.studywithme.$($kv.Value).controller.$cls")
}
$replacements = $replacements | Sort-Object { -$_[0].Length }

$srcRoot = Resolve-Path (Join-Path $JAVA "..\..\..\..\..")
$scanDirs = @(
    (Join-Path $srcRoot "main\java")
    (Join-Path $srcRoot "test\java")
)
foreach ($dir in $scanDirs) {
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem $dir -Recurse -Filter "*.java" | ForEach-Object {
        $t = Get-Content $_.FullName -Raw -Encoding UTF8
        $o = $t
        foreach ($pair in $replacements) {
            $t = $t.Replace($pair[0], $pair[1])
        }
        if ($t -ne $o) {
            $utf8 = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($_.FullName, $t, $utf8)
        }
    }
}

foreach ($folder in @("entity","repository","service")) {
    $d = Join-Path $JAVA $folder
    if ((Test-Path $d) -and -not (Get-ChildItem $d -ErrorAction SilentlyContinue)) { Remove-Item $d -Force }
}

Write-Host "Migration complete."
