/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.validators;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.service.SyncService;
import org.wso2.patchvalidator.productMapper.ProductDownloadNameDefiner;

/**
 * TODO: Class level comment.
 */
public class UpdateValidator {

    private Properties prop = new Properties();
    private static final Logger LOG = LoggerFactory.getLogger(UpdateValidator.class);
    public String updateUrl="null";
    public String updateName="null";
    public String updateDestination="null";



    public String zipUpdateValidate(String updateId, String version, int state,int type,String product,String developedBy) throws IOException, InterruptedException {

        LOG.info("Update Validation Service running");
        prop.load(UpdateValidator.class.getClassLoader().getResourceAsStream("application.properties"));

        String typeof=null;
        if (type==2 || type==3) {
            typeof="update";
        }

        //define download svn url and destination directories and file names
        ZipDownloadPath zipDownloadPath  = new ZipDownloadPath(typeof,version,updateId);

        String filepath=zipDownloadPath .getFilepath();
        updateUrl=zipDownloadPath .getUrl();
        updateDestination=zipDownloadPath .getZipDownloadDestination();
        String destFilePath=zipDownloadPath .getDestFilePath();
        updateName = prop.getProperty("orgUpdate") + version + "-" + updateId;

        String errorMessage = "";
        StringBuilder outMessage = new StringBuilder();
        version = prop.getProperty(version);
        if (version == null) {
            return "Incorrect directory";
        }


        PatchValidateFactory patchValidateFactory = PatchValidator.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        //use commonValidator methods
        String result = commonValidator.downloadZipFile(updateUrl, version, updateId, updateDestination);
        if (!Objects.equals(result, "")) {
            LOG.info(result);
            return result + "\n" + errorMessage;
        }


        //check sign status of the update

//        File fl = new File(updateDestination);
//        for (File file : fl.listFiles()) {
//            if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
//                    || file.getName().endsWith((".sha1"))) {
//                errorMessage = "update" + updateId + " is already signed\n";
//                FileUtils.deleteDirectory(new File(destFilePath));
//                LOG.info(errorMessage);
//                return errorMessage;
//            }
//        }

        //update validation using WUM validator
        String updateValidateScriptPath =prop.getProperty("updateValidateScriptPath");
        String productDownloadPath=prop.getProperty("productDestinationPath");

        ProductDownloadNameDefiner productDownloadNameDefiner = new ProductDownloadNameDefiner();
        String productUrl = productDownloadNameDefiner.findProductUrl(product);

        boolean check = new File(productDownloadPath, "wso2"+product+".zip").exists();

        if (!productUrl.equals("") && !check){
            try {
                LOG.info("Product pack downloading...");
                Process executor = Runtime.getRuntime().exec("bash " + productDownloadPath + "download-product.sh " + productUrl);
                executor.waitFor();
            }catch (InterruptedException e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        try {

            Process executor = Runtime.getRuntime().exec(updateValidateScriptPath+"wum-uc validate "+
                    filepath+" "+productDownloadPath+"wso2"+product+".zip");
            executor.waitFor();

            BufferedReader validateMessage = new BufferedReader(new InputStreamReader(executor.getInputStream()));
            String validateReturn = null;
            while ((validateReturn = validateMessage.readLine()) != null) {
                System.out.println(validateReturn);
                outMessage.append(validateReturn);
            }
            //return result got from the WUM validator
            return outMessage.toString();

        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//        if (Objects.equals(outMessage.toString(), outMessage.toString().contains("'WSO2-CARBON-UPDATE-"+version+"-"+updateId+
//                "' validation successfully finished")) && Objects.equals(errorMessage,"")) {
//            return Constants.SUCCESSFULLY_SIGNED;
//        }

        FileUtils.deleteDirectory(new File(destFilePath));
        LOG.info(errorMessage + "\n");
        return errorMessage;

    }
}


