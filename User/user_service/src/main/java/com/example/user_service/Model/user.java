package com.example.user_service.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class user {

   @Id
   private String userid;
   private String name;
   private String email;

   protected user() {
   }

   public user(String userid, String name, String email) {
      this.userid = userid;
      this.name = name;
      this.email = email;
   }

   public String getUserid() {
      return userid;
   }

   public String getName() {
      return name;
   }

   public String getEmail() {
      return email;
   }

   public void setUserid(String userid) {
      this.userid = userid;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setEmail(String email) {
      this.email = email;
   }
}

