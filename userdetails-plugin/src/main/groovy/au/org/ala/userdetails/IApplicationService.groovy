package au.org.ala.userdetails

interface IApplicationService {

    /***
     * This method is used to generate an api key for a given aws apigateway usage plan
     * @param usagePlanId
     * @return
     */
    Map generateApikey(String usagePlanId)

    /***
     * This method is used to get registered api keys of a user
     * @param userId
     * @return
     */
    def getApikeys(String userId)

    /***
     * This method is used to generate an oauth client
     * @param userId
     * @return
     */
    def generateClient(String userId, List<String> callbackURLs, boolean forGalah)

}