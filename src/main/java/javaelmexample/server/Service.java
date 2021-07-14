package javaelmexample.server;

import functionalj.result.Result;
import functionalj.stream.StreamPlus;

public interface Service<DATA> {
    
    public Class<DATA> dataClass();
    
    public Result<DATA> get(String id);
    
    public StreamPlus<DATA> list();
    
    public Result<DATA> post(DATA data);
    
    public Result<DATA> put(String id, DATA data);
    
    public Result<DATA> delete(String id);
    
}
