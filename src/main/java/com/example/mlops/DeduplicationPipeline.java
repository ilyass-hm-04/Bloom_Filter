package com.example.mlops;

public class DeduplicationPipeline {

    private BloomFilter filter;


    public void process(DataRecord record){
        String id = record.recordId();

        if(filter.mightContain(id)){

        }
        else {

        }


    }

}
