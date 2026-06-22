package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingResponseDTO {
    
    @JsonProperty("address")
    private Address address;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("town")
        private String town;
        
        @JsonProperty("village")
        private String village;
        
        @JsonProperty("state")
        private String state;

        public String getCity() {
            if (city != null) {
                return city;
            }
            if (town != null) {
                return town;
            }
            return village;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getTown() {
            return town;
        }

        public void setTown(String town) {
            this.town = town;
        }

        public String getVillage() {
            return village;
        }

        public void setVillage(String village) {
            this.village = village;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }
}
