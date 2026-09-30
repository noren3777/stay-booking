package dev.noren.staybooking.identity.internal.application.provider;

public interface AccessTokenIssuer {

    String createAccessToken(Actor actor);

}
