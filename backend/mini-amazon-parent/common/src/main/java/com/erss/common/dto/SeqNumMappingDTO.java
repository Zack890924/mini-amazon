package com.erss.common.dto;

public class SeqNumMappingDTO {
    private long seqNum;
    private long packageId;

    public SeqNumMappingDTO(){
}
    public SeqNumMappingDTO(long seqNum, long packageId){

        this.seqNum = seqNum;
        this.packageId = packageId;
    }
    public long getSeqNum(){
         return seqNum;
    }
        public void setSeqNum(long seqNum){
        this.seqNum = seqNum;
    }
        public long getPackageId(){
        return packageId;
    }
        public void setPackageId(long packageId){
        this.packageId = packageId;
    }
}