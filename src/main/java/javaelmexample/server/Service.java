package javaelmexample.server;

import java.util.Optional;
import java.util.stream.Stream;

public interface Service<DATA> {
    
    public Optional<DATA> get(String id);
    
    public Stream<DATA> get();
    
    public DATA post(DATA data);
    
    public Optional<DATA> delete(String id);
    
}
