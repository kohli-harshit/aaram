package model;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;

import com.sun.codemodel.JCodeModel;

public class JsonToPojo {
	static Logger logger = Logger.getLogger(JsonToPojo.class);
	/**
     * This method will generate the POJO files for given json file.
     * @param outputDirectory - where to write the java files
     * @param packageName - package name for java files
     * @param className - class name for main Pojo file
     * @throws IOException
     */
    public void createPojoFile(String jsonString, File outputDirectory, String packageName, String className,SourceType sourceType) throws IOException {
        logger.info("Inside JsonToPojo.java - starting pojo file creation.");
    	JCodeModel codeModel = new JCodeModel();
        GenerationConfig config = new DefaultGenerationConfig() {
            /*
             * @Override public boolean isGenerateBuilders() { // set config
             * option by // overriding method return true; }
             */
            public SourceType getSourceType() {
                return sourceType;
            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return false;
            }
            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false;
            }
        };

        SchemaMapper mapper = new SchemaMapper(
                new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());

        mapper.generate(codeModel, className, packageName, jsonString);
        codeModel.build(outputDirectory);
        logger.info("Inside JsonToPojo.java - pojo file created.");
    }



}
