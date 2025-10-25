package org.springframework.samples.petclinic.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class HelloController {

    @GetMapping("/hello")
    Map<String, String> sayHello() {

        // Get current size of heap in bytes.
        long heapSize = Runtime.getRuntime().totalMemory();

// Get maximum size of heap in bytes. The heap cannot grow beyond this size.
// Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();

// Get amount of free memory within the heap in bytes. This size will
// increase after garbage collection and decrease as new objects are created.
        long heapFreeSize = Runtime.getRuntime().freeMemory();
        Map<String, String> map = new HashMap<>();
        map.put("heapSize:- " , String.valueOf((heapSize/ 1024)/1024));
        map.put("heapMaxSize:- " , String.valueOf((heapMaxSize/ 1024)/1024));
        map.put("heapFreeSize:- " , String.valueOf((heapFreeSize/ 1024)/1024));
        return map;
    }

}
