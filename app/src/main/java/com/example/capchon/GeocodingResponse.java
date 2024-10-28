package com.example.capchon;

import java.util.List;

public class GeocodingResponse {
    public List<Address> addresses;

    public static class Address {
        public String roadAddress;
        public double x;
        public double y;
    }

}
