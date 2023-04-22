package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class XcodeCloudForJenkinsBuilder extends Builder implements SimpleBuildStep {

    private final String branchName;

    @DataBoundConstructor
    public XcodeCloudForJenkinsBuilder(String branchName) {
        this.branchName = branchName;
    }

    public String getBranchName() {
        return branchName;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        try {
            listener.getLogger().println("Branch name: " + branchName);
            Repository repository = Git.open(new File(workspace + "/.git")).getRepository();

            Git git = new Git(repository);

            List<Ref> branches = git.branchList().call();
            boolean branchExists = false;
            for (Ref branch : branches) {
                if (branch.getName().equals("refs/heads/" + branchName)) {
                    branchExists = true;
                    break;
                }
            }
            if (branchExists) {
                git.branchDelete()
                        .setBranchNames(branchName)
                        .setForce(true)
                        .call();
                git.branchDelete()
                        .setBranchNames("origin/" + branchName)
                        .setForce(true)
                        .call();
            }

            git.branchCreate()
                    .setName(branchName)
                    .call();
            git.checkout().setName(branchName).call();
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("Created new branch " + branchName)
                    .setAuthor("Jenkins", "null")
                    .setCommitter("Jenkins", "null")
                    .call();

            CredentialsProvider credentialsProvider =
                    new UsernamePasswordCredentialsProvider(env.get("GIT_USERNAME"), env.get("GIT_PASSWORD"));
            String remoteUrl = git.getRepository().getConfig().getString("remote", "origin", "url");
            listener.getLogger().println("Pushing to " + remoteUrl);
            git.push()
                    .setCredentialsProvider(credentialsProvider)
                    .setRemote("origin")
                    .call();

            listener.getLogger().println("Waiting for Xcode Cloud to build...");
            TimeUnit.MINUTES.sleep(1);

            listener.getLogger().println("Deleting branch " + branchName);
            git.branchDelete()
                    .setBranchNames("origin/" + branchName)
                    .setForce(true)
                    .call();
        } catch (GitAPIException e) {
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
