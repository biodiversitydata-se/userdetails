package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.IApplicationService
import au.org.ala.userdetails.IUserService
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.CreateApiKeyRequest
import com.amazonaws.services.apigateway.model.CreateUsagePlanKeyRequest
import com.amazonaws.services.apigateway.model.GetApiKeysRequest
import com.amazonaws.services.apigateway.model.GetApiKeysResult
import org.apache.commons.lang3.NotImplementedException
import org.springframework.beans.factory.annotation.Autowired

class GormApplicationService implements IApplicationService {

    @Autowired
    IUserService userService
    AmazonApiGateway apiGatewayIdp

    @Override
    Map generateApikey(String usagePlanId) {
        if(!usagePlanId){
            return [apikeys:null, err: "No usage plan id to generate api key"]
        }

        def currentUser = userService.currentUser

        CreateApiKeyRequest request = new CreateApiKeyRequest()
        request.enabled = true
        request.customerId = currentUser.userId
        request.name = "API key for user " + currentUser.userId
        def response = apiGatewayIdp.createApiKey(request)

        if(response.getSdkHttpMetadata().httpStatusCode == 201) {
            //add api key to usage plan
            CreateUsagePlanKeyRequest usagePlanKeyRequest = new CreateUsagePlanKeyRequest()
            usagePlanKeyRequest.keyId = response.id
            usagePlanKeyRequest.keyType = "API_KEY"
            usagePlanKeyRequest.usagePlanId = usagePlanId
            apiGatewayIdp.createUsagePlanKey(usagePlanKeyRequest)

            return [apikeys:getApikeys(currentUser.userId), err: null]
        }
        else{
            return [apikeys:null, err: "Could not generate api key"]
        }
    }

    @Override
    def getApikeys(String userId) {
        GetApiKeysRequest getApiKeysRequest = new GetApiKeysRequest().withCustomerId(userId).withIncludeValues(true)
        GetApiKeysResult response = apiGatewayIdp.getApiKeys(getApiKeysRequest)
        if(response.getSdkHttpMetadata().httpStatusCode == 200){
            return response.items.value
        }
        else{
            return null
        }
    }

    @Override
    def generateClient(String userId, List<String> callbackURLs, boolean forGalah){
        throw new NotImplementedException()
    }
}
