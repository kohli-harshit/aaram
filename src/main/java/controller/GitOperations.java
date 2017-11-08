package controller;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import view.Main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GitOperations {

    public static final String apiARTRepoLocation = "https://github.com/somerepo.git";
    public static String cloneResult="";
    public static String cloneDir="";

    final static Logger logger = Logger.getLogger(GitOperations.class);


    public static void cloneApiART(String folderPath) throws Exception
    {
        cloneDir = folderPath + "_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());;
        File dir = new File(cloneDir);
        Git git = Git.cloneRepository()
                .setURI( apiARTRepoLocation )
                .setDirectory(dir)
                .call();
        logger.info("Git Clone from " + apiARTRepoLocation + " successfull");
        git.close();
        FileUtils.forceDelete(new File(cloneDir + "/.git"));
        cloneResult = String.valueOf(new File(cloneDir + "/pom.xml").exists());
        logger.info("POM.xml present = " + cloneResult);
        if(cloneResult.equals("false"))
        {
            throw new Exception("POM.xml not found in " + folderPath);
        }
    }
}
