package com.github.chemikadze.pumploader;

import org.jstrava.entities.athlete.Athlete;

public class StravaAuthResponse {
    String access_token;
    Athlete athlete;

    public String getAccess_token() { return access_token; }
    public void setAccess_token(String token) { this.access_token = token; }

    public Athlete getAthlete() { return athlete; }
    public void setAthlete() { this.athlete = athlete; }
}
