package com.perago.test;

import java.util.List;
import java.util.Map;

public class Test {

	public static void main(String[] args) {
		Person a = new Person();
		a.setName("Arnold");
		a.setSurname("Schwarzenegger");
	
		Person b = new Person();
		b.setName("Tom");
		b.setSurname("Cruise");
	
		Person c = new Person();
		c.setName("Sylvester");
		c.setSurname("Stallone");
	
		b.setFriend(c);
	
		DiffEngine de = new DiffEngine();
		//Diff diff = de.calculate(a, b);
                Map diff = de.calculate(a, b);
	
		System.out.println("simpleTest Diff");
		printDiff(diff);
	
		a = de.apply(a, b);
	
        System.out.println("a == b : " + a.equals(b));
	}

    private static void printDiff(Map<String, Object> diffs) {
        for (String key : diffs.keySet()) {
            if(diffs.get(key) instanceof List){
                List list = (List) diffs.get(key);
                System.out.println(list.get(0) + key + " was [" + list.get(1)+"] now ["+ list.get(2)+"]");
            }else
            System.out.println(key + " = " + diffs.get(key));
        }
    }
}
