package au.org.ala.userdetails

/**
 * Abstract checking whether a host is allowed
 */
interface IAuthorisedSystemRepository {

    /**
     * Return true if the given host parameter is authorised
     * @param host
     * @return
     */
    Boolean findByHost(String host)

}
