package dev.noren.staybooking.identity.internal.application.provider;

public interface CredentialsAuthenticator {

    Actor authenticate(String email, String password);

}
