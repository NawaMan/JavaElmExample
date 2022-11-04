package javaelmexample.server;

import functionalj.list.FuncList;
import functionalj.promise.Promise;

/**
 * This interface provider abstraction to a REST service.
 * The aims is to allow the service to be implemented without dealing with HTTP.
 **/
public interface RestService<DATA extends RestData> {
    
    /** @return  the class of the data that this service serve. */
    public Class<DATA> dataClass();
    
    /** Get the data by its ID. */
    public Promise<DATA> get(String id);
    
    // TODO - add filter.
    /** Get list of all data */
    public Promise<FuncList<DATA>> list();
    
    /** Add a new data and @return that data */
    public Promise<DATA> post(DATA data);
    
    /** Replace an existing data and @return that data. */
    public Promise<DATA> put(String id, DATA data);
    
    /** Delete an existing data and @return that data. */
    public Promise<DATA> delete(String id);
    
}
