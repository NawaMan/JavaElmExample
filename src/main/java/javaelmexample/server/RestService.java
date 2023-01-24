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
    public default Promise<DATA> get(String id) {
        throw new UnsupportedHttpMethodException();
    }
    
    // TODO - add filter.
    
    /** Get list of all data */
    public default Promise<FuncList<DATA>> list() {
        throw new UnsupportedHttpMethodException();
    }
    
    /** Add a new data and @return that data */
    public default Promise<DATA> post(DATA data) {
        throw new UnsupportedHttpMethodException();
    }
    
    /** Replace an existing data and @return that data. */
    public default Promise<DATA> put(String id, DATA data) {
        throw new UnsupportedHttpMethodException();
    }
    
    /** Delete an existing data and @return that data. */
    public default Promise<DATA> delete(String id) {
        throw new UnsupportedHttpMethodException();
    }
    
}
