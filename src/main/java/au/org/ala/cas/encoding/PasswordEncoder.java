package au.org.ala.cas.encoding;

public interface PasswordEncoder {
    String encode(String password);
    Boolean matches(String plainPassword, String hashedPassword);
}
