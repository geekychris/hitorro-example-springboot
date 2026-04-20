package com.hitorro.example.controller;

import com.hitorro.gittools.commits.CommitSelector;
import com.hitorro.gittools.git.GitCredentials;
import com.hitorro.gittools.git.GitService;
import com.hitorro.gittools.model.*;
import com.hitorro.gittools.scanner.RepositoryScanner;
import com.hitorro.gittools.scanner.ScanConfig;
import com.hitorro.gittools.tagger.RepositoryTagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;

/**
 * Git repository management, commit browsing, tagging, and PR tools.
 */
@RestController
@RequestMapping("/api/gittools")
@CrossOrigin(origins = "*")
public class GitToolsController {

    private static final Logger log = LoggerFactory.getLogger(GitToolsController.class);
    private final GitService gitService = new GitService();
    private final RepositoryScanner scanner = new RepositoryScanner(gitService);
    private final CommitSelector commitSelector = new CommitSelector(gitService);
    private final RepositoryTagger tagger;

    private List<GitRepository> cachedRepos = new ArrayList<>();
    private final GitCredentials credentials = new GitCredentials();
    private List<String> lastScanRoots = new ArrayList<>();

    public GitToolsController() {
        Path metaFile = Path.of("data/git-metadata.json");
        this.tagger = new RepositoryTagger(metaFile);
        // Load token from env first
        String envToken = System.getenv("GITHUB_TOKEN");
        if (envToken != null) credentials.setGithubToken(envToken);
        loadConfig();
        gitService.setCredentials(credentials);
    }

    private Path configFile() { return Path.of("data/gittools-config.json"); }

    private void loadConfig() {
        try {
            if (java.nio.file.Files.exists(configFile())) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var config = mapper.readTree(configFile().toFile());
                if (config.has("githubToken") && !config.get("githubToken").asText().isBlank()) {
                    credentials.setGithubToken(config.get("githubToken").asText());
                }
                if (config.has("githubUsername") && !config.get("githubUsername").asText().isBlank()) {
                    credentials.setGithubUsername(config.get("githubUsername").asText());
                }
                if (config.has("sshKeyPath") && !config.get("sshKeyPath").asText().isBlank()) {
                    credentials.setSshKeyPath(config.get("sshKeyPath").asText());
                }
                if (config.has("gitEmail") && !config.get("gitEmail").asText().isBlank()) {
                    credentials.setGitEmail(config.get("gitEmail").asText());
                }
                if (config.has("gitName") && !config.get("gitName").asText().isBlank()) {
                    credentials.setGitName(config.get("gitName").asText());
                }
                if (config.has("scanRoots") && config.get("scanRoots").isArray()) {
                    this.lastScanRoots = new ArrayList<>();
                    config.get("scanRoots").forEach(n -> lastScanRoots.add(n.asText()));
                }
                // Auto-scan if we have saved roots
                if (!lastScanRoots.isEmpty()) {
                    List<Path> rootPaths = lastScanRoots.stream().map(Path::of).toList();
                    var scanConfig = new ScanConfig(rootPaths, 4,
                            List.of("node_modules", ".m2", "target", "build", "venv", "__pycache__", "dist"));
                    cachedRepos = scanner.scanWithMetadata(scanConfig, tagger);
                    log.info("Auto-loaded {} repositories from saved scan roots", cachedRepos.size());
                }
            }
        } catch (Exception e) {
            log.debug("No saved gittools config: {}", e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            java.nio.file.Files.createDirectories(configFile().getParent());
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            var node = mapper.createObjectNode();
            if (credentials.getGithubToken() != null) node.put("githubToken", credentials.getGithubToken());
            if (credentials.getGithubUsername() != null) node.put("githubUsername", credentials.getGithubUsername());
            if (credentials.getSshKeyPath() != null) node.put("sshKeyPath", credentials.getSshKeyPath());
            if (credentials.getGitEmail() != null) node.put("gitEmail", credentials.getGitEmail());
            if (credentials.getGitName() != null) node.put("gitName", credentials.getGitName());
            var arr = node.putArray("scanRoots");
            lastScanRoots.forEach(arr::add);
            mapper.writeValue(configFile().toFile(), node);
        } catch (Exception e) {
            log.warn("Failed to save gittools config: {}", e.getMessage());
        }
    }

    // ─── Scan & List ─────────────────────────────────────────────

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> roots = (List<String>) body.getOrDefault("roots",
                List.of(System.getProperty("user.home")));
        int maxDepth = body.containsKey("maxDepth") ? ((Number) body.get("maxDepth")).intValue() : 4;

        List<Path> rootPaths = roots.stream().map(Path::of).toList();
        var config = new ScanConfig(rootPaths, maxDepth,
                List.of("node_modules", ".m2", "target", "build", "venv", "__pycache__", "dist"));

        cachedRepos = scanner.scanWithMetadata(config, tagger);
        lastScanRoots = roots;
        saveConfig();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", cachedRepos.size());
        result.put("repos", repoSummaries(cachedRepos));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/repos")
    public ResponseEntity<List<Map<String, Object>>> listRepos(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        var filtered = new ArrayList<>(cachedRepos);
        if (tag != null && !tag.isBlank()) {
            filtered.removeIf(r -> r.tags() == null || !r.tags().contains(tag));
        }
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            filtered.removeIf(r -> !r.name().toLowerCase().contains(lower)
                    && (r.description() == null || !r.description().toLowerCase().contains(lower)));
        }
        if ("date".equals(sort)) {
            filtered.sort(Comparator.comparing(
                    r -> r.lastCommitDate() != null ? r.lastCommitDate() : java.time.Instant.EPOCH,
                    Comparator.reverseOrder()));
        } else {
            filtered.sort(Comparator.comparing(r -> r.name().toLowerCase()));
        }
        return ResponseEntity.ok(repoSummaries(filtered));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> allTags() {
        return ResponseEntity.ok(tagger.getAllTags());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("cachedRepos", cachedRepos.size());
        s.put("tags", tagger.getAllTags());
        s.putAll(getCredentialsStatus());
        s.put("scanRoots", lastScanRoots);
        return ResponseEntity.ok(s);
    }

    @PostMapping("/config/credentials")
    public ResponseEntity<?> setCredentials(@RequestBody Map<String, String> body) {
        if (body.containsKey("githubToken")) credentials.setGithubToken(body.get("githubToken"));
        if (body.containsKey("githubUsername")) credentials.setGithubUsername(body.get("githubUsername"));
        if (body.containsKey("sshKeyPath")) credentials.setSshKeyPath(body.get("sshKeyPath"));
        if (body.containsKey("gitEmail")) credentials.setGitEmail(body.get("gitEmail"));
        if (body.containsKey("gitName")) credentials.setGitName(body.get("gitName"));
        saveConfig();
        return ResponseEntity.ok(getCredentialsStatus());
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.putAll(getCredentialsStatus());
        cfg.put("scanRoots", lastScanRoots);
        return ResponseEntity.ok(cfg);
    }

    private Map<String, Object> getCredentialsStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("githubTokenSet", credentials.hasToken());
        s.put("githubUsername", credentials.getGithubUsername());
        s.put("sshKeySet", credentials.hasSshKey());
        s.put("gitEmail", credentials.getGitEmail());
        s.put("gitName", credentials.getGitName());
        return s;
    }

    // ─── Remote Operations ───────────────────────────────────────

    @PostMapping("/repos/{index}/fetch")
    public ResponseEntity<?> fetchRepo(@PathVariable int index) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        var result = gitService.fetch(cachedRepos.get(index).path());
        return ResponseEntity.ok(Map.of("success", result.isSuccess(),
                "stdout", result.stdout(), "stderr", result.stderr()));
    }

    @PostMapping("/repos/{index}/pull")
    public ResponseEntity<?> pullRepo(@PathVariable int index) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        var result = gitService.pull(cachedRepos.get(index).path());
        return ResponseEntity.ok(Map.of("success", result.isSuccess(),
                "stdout", result.stdout(), "stderr", result.stderr()));
    }

    @PostMapping("/clone")
    public ResponseEntity<?> cloneRepo(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        String targetDir = body.get("targetDir");
        if (url == null || targetDir == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "url and targetDir required"));
        }
        try {
            gitService.clone(url, Path.of(targetDir));
            return ResponseEntity.ok(Map.of("success", true, "path", targetDir));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ─── Repo Details ────────────────────────────────────────────

    @GetMapping("/repos/{index}")
    public ResponseEntity<?> repoDetail(@PathVariable int index) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        var repo = cachedRepos.get(index);
        var stats = gitService.getRepoStats(repo.path());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("repo", repoSummary(repo, index));
        detail.put("stats", stats);
        detail.put("remotes", gitService.listRemotes(repo.path()));
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/repos/{index}/branches")
    public ResponseEntity<?> branches(@PathVariable int index) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(gitService.listBranches(cachedRepos.get(index).path()));
    }

    @GetMapping("/repos/{index}/tags")
    public ResponseEntity<?> repoTags(@PathVariable int index) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(gitService.listTags(cachedRepos.get(index).path()));
    }

    @GetMapping("/repos/{index}/commits")
    public ResponseEntity<?> commits(@PathVariable int index,
                                      @RequestParam(defaultValue = "30") int maxCount,
                                      @RequestParam(required = false) String branch,
                                      @RequestParam(required = false) String author,
                                      @RequestParam(required = false) String message) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        var commits = commitSelector.selectCommits(
                cachedRepos.get(index).path(), branch, maxCount, author, message, null, null);
        return ResponseEntity.ok(commits);
    }

    @GetMapping("/repos/{index}/commits/{hash}")
    public ResponseEntity<?> commitDetail(@PathVariable int index, @PathVariable String hash) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        var commit = gitService.getCommit(cachedRepos.get(index).path(), hash);
        if (commit == null) return ResponseEntity.notFound().build();
        var files = gitService.getChangedFiles(cachedRepos.get(index).path(), hash);
        return ResponseEntity.ok(Map.of("commit", commit, "files", files));
    }

    @GetMapping("/repos/{index}/commits/{hash}/diff")
    public ResponseEntity<?> commitDiff(@PathVariable int index, @PathVariable String hash) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        try {
            String diff = gitService.diff(cachedRepos.get(index).path(), hash);
            return ResponseEntity.ok(Map.of("diff", diff));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    // ─── Tagging & Description ───────────────────────────────────

    @PostMapping("/repos/{index}/tag")
    public ResponseEntity<?> addTag(@PathVariable int index, @RequestBody Map<String, String> body) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        tagger.addTag(cachedRepos.get(index).path(), body.get("tag"));
        return ResponseEntity.ok(Map.of("tags", tagger.getMetadata(cachedRepos.get(index).path()).getTags()));
    }

    @DeleteMapping("/repos/{index}/tag/{tag}")
    public ResponseEntity<?> removeTag(@PathVariable int index, @PathVariable String tag) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        tagger.removeTag(cachedRepos.get(index).path(), tag);
        return ResponseEntity.ok(Map.of("tags", tagger.getMetadata(cachedRepos.get(index).path()).getTags()));
    }

    @PostMapping("/repos/{index}/description")
    public ResponseEntity<?> setDescription(@PathVariable int index, @RequestBody Map<String, String> body) {
        if (index < 0 || index >= cachedRepos.size()) return ResponseEntity.notFound().build();
        tagger.setDescription(cachedRepos.get(index).path(), body.get("description"));
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private List<Map<String, Object>> repoSummaries(List<GitRepository> repos) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < repos.size(); i++) {
            list.add(repoSummary(repos.get(i), i));
        }
        return list;
    }

    private Map<String, Object> repoSummary(GitRepository r, int index) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("index", index);
        info.put("name", r.name());
        info.put("path", r.path().toString());
        info.put("description", r.description());
        info.put("tags", r.tags());
        info.put("currentBranch", r.currentBranch());
        info.put("branchCount", r.branchCount());
        info.put("tagCount", r.tagCount());
        info.put("remoteUrl", r.remoteUrl());
        info.put("lastCommitHash", r.lastCommitHash());
        info.put("lastCommitMessage", r.lastCommitMessage());
        info.put("lastCommitDate", r.lastCommitDate());
        info.put("lastCommitAuthor", r.lastCommitAuthor());
        return info;
    }
}
