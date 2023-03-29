package au.org.ala.userdetails

import com.amazonaws.AmazonWebServiceResult
import com.amazonaws.ResponseMetadata
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.CreateApiKeyRequest
import com.amazonaws.services.apigateway.model.CreateUsagePlanKeyRequest
import com.amazonaws.services.apigateway.model.GetApiKeysRequest
import com.amazonaws.services.apigateway.model.GetApiKeysResult
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.CreateUserPoolClientRequest
import com.amazonaws.services.cognitoidp.model.CreateUserPoolClientResult
import grails.config.Config
import org.springframework.beans.factory.annotation.Autowired

class CognitoApplicationService implements IApplicationService {

    @Autowired
    IUserService userService
    AWSCognitoIdentityProvider cognitoIdp
    String poolId
    AmazonApiGateway apiGatewayIdp
    Config config

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

        if(isSuccessful(response)) {
            //add api key to usage plan
            CreateUsagePlanKeyRequest usagePlanKeyRequest = new CreateUsagePlanKeyRequest()
            usagePlanKeyRequest.keyId = response.id
            usagePlanKeyRequest.keyType = "API_KEY"
            usagePlanKeyRequest.usagePlanId = usagePlanId
            apiGatewayIdp.createUsagePlanKey(usagePlanKeyRequest)

            return [apikeys:getApikeys(currentUser.userId), error: null]
        }
        else{
            return [apikeys:null, error: "Could not generate api key"]
        }
    }

    @Override
    def getApikeys(String userId) {

        GetApiKeysRequest getApiKeysRequest = new GetApiKeysRequest().withCustomerId(userId).withIncludeValues(true)
        GetApiKeysResult response = apiGatewayIdp.getApiKeys(getApiKeysRequest)
        if(isSuccessful(response)){
            return response.items.value
        }
        else{
            return null
        }
    }

    @Override
    def generateClient(String userId, List<String> callbackURLs, boolean forGalah) {
        CreateUserPoolClientRequest request =  new CreateUserPoolClientRequest().withUserPoolId(poolId)
        request.clientName = "Client for user " + userId
        request.allowedOAuthFlows = ["code"]
        request.generateSecret = false
        request.supportedIdentityProviders = config.getProperty('oauth.support.dynamic.client.supportedIdentityProviders', List, [])
        request.preventUserExistenceErrors = "ENABLED"
        request.explicitAuthFlows = config.getProperty('oauth.support.dynamic.client.authFlows', List, [])
        request.allowedOAuthFlowsUserPoolClient = true

        def scopes = config.getProperty('oauth.support.dynamic.client.scopes', List, [])

        if(scopes) {
            request.allowedOAuthScopes = scopes
        }

        request.callbackURLs = callbackURLs
        if(forGalah) {
            request.callbackURLs.addAll(config.getProperty('oauth.support.dynamic.client.galah.callbackURLs', List, []))
        }

        CreateUserPoolClientResult response = cognitoIdp.createUserPoolClient(request)

        if(isSuccessful(response)){
            //update user custom attribute with new clientId
            userService.addOrUpdateProperty(userService.currentUser, "clientId", response.userPoolClient.clientId)
            return [apikeys: response.userPoolClient.clientId, error: null]
        }
        else{
            return [clientId: null, error: "Could not generate client"]
        }
    }

    private boolean isSuccessful(AmazonWebServiceResult<? extends ResponseMetadata> result) {
        def code = result.sdkHttpMetadata.httpStatusCode
        return code >= 200 && code < 300
    }
}
