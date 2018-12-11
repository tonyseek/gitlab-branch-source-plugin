package argelbargel.jenkins.plugins.gitlab_branch_source.heads;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.browser.GitLab;
import hudson.plugins.git.extensions.GitSCMExtension;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


abstract class GitLabSCMHeadImpl extends GitLabSCMHead {
    private final int projectId;
    private final SCMRevisionImpl revision;
    private final String pronoun;
    private final GitLabSCMRefSpec refSpec;
    private transient Map<Integer, GitLabProject> projectCache;

    GitLabSCMHeadImpl(int projectId, @Nonnull String name, @Nonnull String hash, @Nonnull String pronoun, @Nonnull GitLabSCMRefSpec refSpec) {
        super(refSpec.remoteName(name));
        this.projectId = projectId;
        this.revision = new SCMRevisionImpl(this, hash);
        this.pronoun = pronoun;
        this.refSpec = refSpec;
    }

    @Override
    @CheckForNull
    public final String getPronoun() {
        return pronoun;
    }

    @Nonnull
    @Override
    public final SCMRevisionImpl getRevision() {
        return revision;
    }

    @Nonnull
    @Override
    final GitLabSCMRefSpec getRefSpec() {
        return refSpec;
    }

    @Override
    public final int getProjectId() {
        return projectId;
    }

    @Nonnull
    @Override
    public String getRef() {
        return refSpec.destinationRef(getName());
    }

    @Nonnull
    public final GitSCM createSCM(GitLabSCMSource source) {
        try {
            return new GitSCM(getRemotes(source), getBranchSpecs(),
                    false, Collections.<SubmoduleConfig>emptyList(),
                    getBrowser(source.getProjectId(), source), null, getExtensions(source));
        } catch (Exception e) {
            throw new RuntimeException("error creating scm for source + " + source.getId(), e);
        }
    }

    @Nonnull
    List<UserRemoteConfig> getRemotes(@Nonnull GitLabSCMSource source) throws GitLabAPIException {
        return singletonList(
                new UserRemoteConfig(
                        getProject(projectId, source).getRemote(source),
                        "origin", getRefSpec().delegate().toString(),
                        source.getCredentialsId()));
    }

    @Nonnull
    List<BranchSpec> getBranchSpecs() {
        return singletonList(new BranchSpec(getRef()));
    }

    @Nonnull
    List<GitSCMExtension> getExtensions(GitLabSCMSource source) {
        return emptyList();
    }

    final GitLabProject getProject(int projectId, GitLabSCMSource source) throws GitLabAPIException {
        if (projectCache == null) {
            projectCache = new HashMap<>(1, 1.0f);
        }

        if (!projectCache.containsKey(projectId)) {
            projectCache.put(projectId, gitLabAPI(source.getSourceSettings()).getProject(projectId));
        }

        return projectCache.get(projectId);
    }

    // TODO: do we need this? Would prefer it to stay in GitLabSCMSource only
    private GitLab getBrowser(int projectId, @Nonnull GitLabSCMSource source) throws GitLabAPIException {
        String version;
        try {
            version = gitLabAPI(source.getSourceSettings()).getVersion();
        } catch (GitLabAPIException e) {
            version = "8.3.2";
        }
        return new GitLab(getProject(projectId, source).getWebUrl(), version);
    }
}
