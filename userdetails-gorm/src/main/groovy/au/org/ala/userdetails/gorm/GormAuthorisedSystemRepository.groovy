package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.IAuthorisedSystemRepository

class GormAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    @Override
    Boolean findByHost(String host) {
        return AuthorisedSystem.findByHost(host) != null
    }

}
