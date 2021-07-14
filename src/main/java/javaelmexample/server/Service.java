package javaelmexample.server;

import functionalj.list.FuncList;
import functionalj.promise.Promise;

public interface Service<DATA> {
    
    public Class<DATA> dataClass();
    
    public Promise<DATA> get(String id);
    
    public Promise<FuncList<DATA>> list();
    
    public Promise<DATA> post(DATA data);
    
    public Promise<DATA> put(String id, DATA data);
    
    public Promise<DATA> delete(String id);
    
}
