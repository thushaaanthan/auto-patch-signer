/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/

package org.wso2.patchvalidator.service;

import java.sql.SQLException;
import java.util.Properties;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.wso2.patchvalidator.commiter.KeysCommitter;
import org.wso2.patchvalidator.constants.Constants;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.productMapper.ProductDefiner;
import org.wso2.patchvalidator.updator.UIUpdater;
import org.wso2.patchvalidator.validators.PatchValidator;


import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import org.wso2.patchvalidator.validators.UpdateValidator;
import org.wso2.patchvalidator.store.pmtDataObjectImple;
import org.wso2.patchvalidator.validators.ValidateAndMailSender;


@Path("/request")
public class SyncService {
    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);
    private String status="";
    private Properties prop = new Properties();

    @POST
    @Path("/service")
    @Produces(MediaType.TEXT_PLAIN)
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postRequest(@FormParam("patchId") String patchId,
                                @FormParam("version") String version,
                                @FormParam("state") Integer state,
                                @FormParam("product") String productName,
                                @FormParam("developedBy") String developedBy) throws IOException,
        InterruptedException, SQLException {

            System.out.println("Request received....!!!");
            prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
            String patchValidateStatus,updateValidateStatus;
            String[] productVersion = productName.split(" ");
            String splitProductName=productName.split("[0-9]")[0];

            ProductDefiner productSelector=new ProductDefiner();
            String product= productSelector.findProduct(splitProductName,productVersion[productVersion.length-1]);
            pmtDataObjectImple pmtData = new pmtDataObjectImple();
            int productType = pmtData.getProductType(product);

            ArrayList<String> toList = new ArrayList<>();
            ArrayList<String> ccList= new ArrayList<>();

            PatchValidator objPatchValidator = new PatchValidator();
            UpdateValidator objUpdateValidator = new UpdateValidator();
            ValidateAndMailSender checkValidity = new ValidateAndMailSender();
            UIUpdater uiUpdator = new UIUpdater();
            pmtData.insertPostRequestParam(patchId, version, state, productType, product, developedBy, status);

            //check product type ( 1 = patch / 2 = update / 3 = patch and update )
            //patch validation
            if (productType == 1) {
                LOG.info("\n\nReceived Patch validation Request");
                patchValidateStatus = objPatchValidator.zipPatchValidate(patchId, version, state, productType, product, developedBy);
                LOG.info("patchValidateStatus = " + patchValidateStatus);

                if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_VALIDATED))) {

                    patchValidateStatus = patchValidateStatus + " and " + KeysCommitter.validateKeysCommit((objPatchValidator.patchUrl.split("carbon/")[1]), objPatchValidator.patchDestination);
                    //String patchName = prop.getProperty("organization") + version + "-" + patchId;
                    //KeysCommitter.updatePMT(patchName, state);
                    setCCList(developedBy, toList, ccList);
                    checkValidity.excuteSendMail(toList, ccList, patchId, patchValidateStatus);

                }
                else {
                        if ((patchValidateStatus.trim().equals("patch" + patchId + Constants.SIGNED))) {

                            setCCList(developedBy, toList, ccList);
                            checkValidity.excuteSendMail(toList, ccList, patchId, patchValidateStatus);
                        }
                        else {
                            setCCList(developedBy, toList, ccList);
                            checkValidity.excuteSendMail(toList, ccList, patchId, patchValidateStatus);
                    }

                }
                //uiUpdator.sendRequest(patchValidateStatus);
                changeValidateStatus(patchId, product, pmtData, patchValidateStatus,version, "patch");

                return Response.ok("patchValidateStatus : "+patchValidateStatus, MediaType.TEXT_PLAIN)
                        .header("Access-Control-Allow-Credentials", true)
                        .build();
            }

            //update validation
            else if (productType == 2) {
                LOG.info("\nReceived Update validation Request");
                updateValidateStatus = objUpdateValidator.zipUpdateValidate(patchId, version, state, productType, product,
                        developedBy);
                LOG.info("updateValidateStatus = " + updateValidateStatus);

                version = getString(version);
                String statusOfUpdateValidation = "[INFO] '"+prop.getProperty("orgUpdate")+version+"-"+patchId+"' "+Constants.UPDATE_VALIDATED;

                if (updateValidateStatus.equals(statusOfUpdateValidation)) {

                    updateValidateStatus = updateValidateStatus + " " + KeysCommitter.validateKeysCommit((objUpdateValidator.updateUrl.split("carbon/")[1]), objUpdateValidator.updateDestination);
                    // String updateName = prop.getProperty("orgPatch") + version + "-" + patchId;
                    // KeysCommitter.updatePMT(updateName, state);

                    setCCList(developedBy, toList, ccList);
                    checkValidity.excuteSendMail(toList, ccList, patchId,  updateValidateStatus);

                } else {
                        if((updateValidateStatus.trim().equals("update"+patchId+Constants.SIGNED))) {

                            setCCList(developedBy, toList, ccList);
                                checkValidity.excuteSendMail(toList, ccList, patchId, updateValidateStatus);
                        }
                        else {
                            setCCList(developedBy, toList, ccList);
                                checkValidity.excuteSendMail(toList, ccList, patchId,updateValidateStatus);
                        }
                    }
                //uiUpdator.sendRequest(updateValidateStatus);
                changeValidateStatus(patchId, product, pmtData, updateValidateStatus,version, "update");


                return Response.ok("UpdateValidateStatus : "+ updateValidateStatus, MediaType.TEXT_PLAIN)
                        .header("Access-Control-Allow-Credentials", true)
                        .build();
            }

            //patch and update validation
            else if (productType == 3) {
            LOG.info("\nReceived Patch and Update validation Request");
            updateValidateStatus = objUpdateValidator.zipUpdateValidate(patchId, version, state, productType, product,
                    developedBy);

            patchValidateStatus = objPatchValidator.zipPatchValidate(patchId, version, state, productType, product,
                    developedBy);

            version = getString(version);
            LOG.info("Patch Validate Status :"+patchValidateStatus);
            LOG.info("Update Validate Status :"+updateValidateStatus);
            String statusOfUpdateValidation= "[INFO] '"+prop.getProperty("orgUpdate")+version+"-"+patchId+"' "+Constants.UPDATE_VALIDATED;
//            if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_SIGNED)) &&
//                    (updateValidateStatus.equals(promptOfUpdateValidate))) {
//                String patchName = prop.getProperty("orgPatch") + version + "-" + patchId;
//
//                System.out.println(objPatchValidator.patchUrl.split("carbon/")[1]);
//                System.out.println(objUpdateValidator.updateUrl.split("carbon/")[1]);
//                KeysCommitter.validateKeysCommit((objPatchValidator.patchUrl.split("carbon/")[1]), objPatchValidator.patchDestination);
//                KeysCommitter.validateKeysCommit((objUpdateValidator.updateUrl.split("carbon/")[1]), objUpdateValidator.updateDestination);
//                //KeysCommitter.updatePMT(patchName, state);
//
//                setCCList(developedBy, toList, ccList);
//                checkValidity.excuteSendMailPatchandUpdate(toList, ccList, patchId,patchValidateStatus,updateValidateStatus);
//
//
//            } else {
//
//                setCCList(developedBy, toList, ccList);
//                checkValidity.excuteSendMailPatchandUpdate(toList, ccList, patchId,patchValidateStatus,updateValidateStatus);
//
//            }
            if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_VALIDATED))) {

                patchValidateStatus = patchValidateStatus + " and " + KeysCommitter.validateKeysCommit((objPatchValidator.patchUrl.split("carbon/")[1]), objPatchValidator.patchDestination);
                //String patchName = prop.getProperty("organization") + version + "-" + patchId;
                //KeysCommitter.updatePMT(patchName, state);

            }
            if (updateValidateStatus.equals(statusOfUpdateValidation)) {

                updateValidateStatus = updateValidateStatus + " " + KeysCommitter.validateKeysCommit((objUpdateValidator.updateUrl.split("carbon/")[1]), objUpdateValidator.updateDestination);
                // String updateName = prop.getProperty("orgPatch") + version + "-" + patchId;
                // KeysCommitter.updatePMT(updateName, state);

            }

            //send email
            setCCList(developedBy, toList, ccList);
            checkValidity.excuteSendMailPatchandUpdate(toList, ccList, patchId,patchValidateStatus,updateValidateStatus);
            //uiUpdator.sendRequest("patchValidateStatus : "+patchValidateStatus +" __ updateValidateStatus : "+ updateValidateStatus);
            changeValidateStatus(patchId, product, pmtData, patchValidateStatus,version, "patch");
            changeValidateStatus(patchId, product, pmtData, updateValidateStatus,version, "update");

            return Response.ok("patchValidateStatus : "+patchValidateStatus +" __ updateValidateStatus : "+ updateValidateStatus, MediaType.TEXT_PLAIN)
                        .header("Access-Control-Allow-Credentials", true)
                        .build();

            }

            return Response.noContent().build();
        }

    private void setCCList(@FormParam("developedBy") String developedBy, ArrayList<String> toList, ArrayList<String> ccList) {
        toList.add(developedBy);
        ccList.add(prop.getProperty("ccList1"));
        ccList.add(prop.getProperty("ccList2"));
    }

    private String getString(@FormParam("version") String version) {
        switch (version) {
            case "wilkes":
                version = "4.4.0";
                break;
            case "perlis":
                version = "4.3.0";
                break;
            case "turing":
                version ="4.2.0";
                break;
            default:
                LOG.error("Error in version of the product");
        }
        return version;
    }

    private void changeValidateStatus(@FormParam("patchId") String patchId, @FormParam("product") String product,
                                      pmtDataObjectImple insertRequestParametersToDB, String patchMessage,String version, String patchType) throws SQLException {
        if (patchMessage.trim().equals(patchType + patchId + Constants.SIGNED) || patchMessage.trim().equals(Constants.SUCCESSFULLY_SIGNED)
                || patchMessage.trim().equals("[INFO] '"+prop.getProperty("orgUpdate")+version+"-"+patchId+"' "+Constants.UPDATE_VALIDATED + " " + Constants.SUCCESSFULLY_KEY_COMMITTED)) {
            LOG.info("Process Successful and status updated..!");
            insertRequestParametersToDB.updatePostRequestStatus(product,patchId,Constants.SUCCESS_STATE);

        } else if (patchMessage.equals(Constants.SVN_CONNECTION_FAIL_STATE)) {
            LOG.info("SVN connection failure.");
            insertRequestParametersToDB.updatePostRequestStatus(product,patchId,Constants.SVN_CONNECTION_FAIL_STATE);
        }
          else if (patchMessage.equals(Constants.COMMIT_KEYS_FAILURE)) {
            LOG.info("Failures while committing keys.");
            insertRequestParametersToDB.updatePostRequestStatus(product,patchId,Constants.COMMIT_KEYS_FAILURE);
        } else if (patchMessage.trim().equals(Constants.VALIDATION_FAIL_STATE)) {
            LOG.info("Validation failed.");
            insertRequestParametersToDB.updatePostRequestStatus(product,patchId,Constants.VALIDATION_FAIL_STATE);
        }
        else{
            LOG.info("Process finished.");
            insertRequestParametersToDB.updatePostRequestStatus(product,patchId,Constants.VALIDATION_FAIL_STATE);
        }
    }


}





