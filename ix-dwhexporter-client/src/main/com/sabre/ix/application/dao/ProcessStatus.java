package com.sabre.ix.application.dao;
// Generated 15.11.2011 16:32:16 by Hibernate Tools 3.2.0.b9


import java.util.Date;

/**
 * ProcessStatus generated by hbm2java
 */
public class ProcessStatus  implements java.io.Serializable {


     private int id;
     private String prozessGruppe;
     private String who;
     private String prozess;
     private Date startTime;
     private Date endTime;
     private String status;

    public ProcessStatus() {
    }

	
    public ProcessStatus(int id, String prozessGruppe, String who) {
        this.id = id;
        this.prozessGruppe = prozessGruppe;
        this.who = who;
    }
    public ProcessStatus(int id, String prozessGruppe, String who, String prozess, Date startTime, Date endTime, String status) {
       this.id = id;
       this.prozessGruppe = prozessGruppe;
       this.who = who;
       this.prozess = prozess;
       this.startTime = startTime;
       this.endTime = endTime;
       this.status = status;
    }
   
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    public String getProzessGruppe() {
        return this.prozessGruppe;
    }
    
    public void setProzessGruppe(String prozessGruppe) {
        this.prozessGruppe = prozessGruppe;
    }
    public String getWho() {
        return this.who;
    }
    
    public void setWho(String who) {
        this.who = who;
    }
    public String getProzess() {
        return this.prozess;
    }
    
    public void setProzess(String prozess) {
        this.prozess = prozess;
    }
    public Date getStartTime() {
        return this.startTime;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    public Date getEndTime() {
        return this.endTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    public String getStatus() {
        return this.status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }




}


