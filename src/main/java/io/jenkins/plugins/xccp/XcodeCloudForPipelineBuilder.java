package io.jenkins.plugins.xccp;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitException;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.jenkinsci.plugins.gitclient.Git;

public class XcodeCloudForPipelineBuilder extends Builder implements SimpleBuildStep {

    private final String branchName;

    @DataBoundConstructor
    public XcodeCloudForPipelineBuilder(String branchName) {
        this.branchName = branchName;
    }

    public String getBranchName() {
        return branchName;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        try {
            listener.getLogger().println("Branch name: " + branchName);
            listener.getLogger().println("Workspace: " + workspace.getRemote());

            GitClient git = Git.with(listener, env).in(workspace).using("git").getClient();
            URIish remoteUrl = new URIish(git.getRemoteUrl("origin"));
            StandardUsernameCredentials credentials =
                    new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "xcode-cloud", "Xcode Cloud", env.get("GIT_USERNAME"), env.get("GIT_PASSWORD"));
            git.setCredentials(credentials);

            boolean branchExists = false;
            for (Branch branch : git.getBranches()) {
                if (branch.getName().equals(branchName)) {
                    branchExists = true;
                    break;
                }
            }
            if (branchExists) {
                git.deleteBranch(branchName);
            }
            boolean remoteBranchExists = false;
            for (Branch branch : git.getRemoteBranches()) {
                listener.getLogger().println("Remote branch: " + branch.getName());
                if (branch.getName().equals("origin/" + branchName)) {
                    remoteBranchExists = true;
                    break;
                }
            }
            if (remoteBranchExists) {
                git.push().to(remoteUrl).ref(":refs/heads/" + branchName).execute();
            }

            git.branch(branchName);
            git.checkout().ref(branchName).execute();
            git.add(".");
            git.commit("Created new branch " + branchName);

            listener.getLogger().println("Pushing to " + remoteUrl);
            git.push().ref(branchName).to(remoteUrl).execute();

            listener.getLogger().println("Waiting for Xcode Cloud to build...");
            TimeUnit.MINUTES.sleep(1);

            listener.getLogger().println("Deleting branch " + branchName);
            git.push().to(remoteUrl).ref(":refs/heads/" + branchName).execute();
        } catch (GitException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Push to Xcode Cloud";
        }

        @Override
        public Builder newInstance(StaplerRequest req, @NonNull JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }
}