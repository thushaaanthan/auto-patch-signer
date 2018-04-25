package org.wso2.patchvalidator.productMapper;

public class ProductDownloadNameDefiner {

    public String findProductUrl(String productAbbreviation){

        String[] productNameArray = productAbbreviation.split("-");
        String productVersion = productNameArray[1];
        String productName = productNameArray[0];
        String productUrl;

        switch (productName) {
            case "am":  productUrl = "api-manager/";
                break;
            case "cep":  productUrl = "complex-event-processor/";
                break;
            case "dss":  productUrl = "data-services-server/";
                break;
            case "emm":  productUrl = "enterprise-mobility-manager/";
                break;
            case "ei":  productUrl = "enterprise-integrator/";
                break;
            case "esb":  productUrl = "enterprise-service-bus/";
                break;
            case "greg":  productUrl = "governance-registry/";
                break;
            case "is":  productUrl = "identity-server/";
                break;
            case "das":  productUrl = "data-analytics-server/";
                break;
            case "is-km":  productUrl = "identity-server-key-manager/";
                break;
            default: productUrl="";
                break;
        }

        if(productUrl.equals("")){
            return "";
        }else{
            productUrl = productUrl + productVersion + "/wso2" + productAbbreviation + ".zip";
            return productUrl;
        }
    }
}
