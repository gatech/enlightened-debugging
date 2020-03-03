/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */


package gov.nasa.jpf.util;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.SystemAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;


public abstract class ObjectList {
  

  private ObjectList(){}
  
  private static class Node implements Cloneable {
    Object data;
    Node next;

    Node(Object data, Node next) {
      this.data = data;
      this.next = next;
    }
    
    @Override
	public boolean equals(Object o){
      if (o instanceof Node){        
        Node n = this;
        Node no = (Node)o;
        for (; n != null && no != null; n=n.next, no=no.next){
          if (!n.data.equals(no.data)){
            return false;
          }
        }
        return (n == null) && (no == null);
      } else {
        return false;
      }
    }
    
    @Override
	protected Node clone(){
      try {
        return (Node)super.clone();
      } catch (CloneNotSupportedException cnsx){
        throw new RuntimeException("Node clone failed");
      }
    }
    

    public Node cloneWithReplacedData (Object oldData, Object newData){
      Node newThis = clone();
      
      if (data.equals(oldData)){
        newThis.data = newData;
        
      } else if (next != null) {
        newThis.next = next.cloneWithReplacedData(oldData, newData);
      }
      
      return newThis;
    }
    
    public Node cloneWithRemovedData (Object oldData){
      Node newThis = clone();
      
      if (next != null){
        if (next.data.equals(oldData)){
          newThis.next = next.next;
        } else {
          newThis.next = next.cloneWithRemovedData( oldData);
        }
      }
      
      return newThis;      
    }
  }

  public static class Iterator implements java.util.Iterator<Object>, Iterable<Object> {
    Object cur;
    
    Iterator (Object head){
      cur = head;
    }
    
    @Override
	public boolean hasNext() {
      return cur != null;      
    }

    @Override
	public Object next() {
      if (cur != null){
        if (cur instanceof Node){
          Node n = (Node)cur;
          cur = n.next;
          return n.data;
          
        } else { 
          Object n = cur;
          cur = null;
          return n;
        }
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
	public void remove() {

      throw new UnsupportedOperationException();
    }
    
    @Override
	public java.util.Iterator<Object> iterator(){
      return this;
    }
  }
  
  static final Iterator emptyIterator = new Iterator(null);
  
  public static Iterator iterator (Object head){
    if (head == null){
      return emptyIterator;
    } else {
      return new Iterator(head);
    }
  }
  
  public static class TypedIterator<A> implements java.util.Iterator<A>, Iterable<A> {
    Object cur;
    Class<A> type;
    
    TypedIterator (Object head, Class<A> type){
      this.type = type;
      this.cur = null;
      
      if (head instanceof Node){
        for (Node n = (Node)head; n != null; n = n.next){
          if (type.isAssignableFrom(n.data.getClass())) {
            cur = n;
            break;
          }
        }
      } else if (head != null) {
        if (type.isAssignableFrom(head.getClass())) {
          cur = head;
        }
      }
    }
    
    @Override
	public boolean hasNext() {
      return cur != null;      
    }

    @Override
	public A next() {
      
      if (cur != null){
        if (cur instanceof Node){
          Node nCur = (Node)cur;
          cur = null;
          A d = (A)nCur.data;
          
          for (Node n=nCur.next; n != null; n=n.next){
            if (type.isAssignableFrom(n.data.getClass())){
              cur = n;
              break;
            }
          }
          
          return d;
          
        } else { 
          A d = (A)cur;
          cur = null;
          return d;
        }
        
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
	public void remove() {

      throw new UnsupportedOperationException();
    }
    
    @Override
	public java.util.Iterator<A> iterator(){
      return this;
    }
  }
  
  static final TypedIterator<Object> emptyTypedIterator = new TypedIterator<Object>(null,Object.class);
  
  public static <A> TypedIterator<A> typedIterator (Object head, Class<A> type){
    if (head == null){
      return (TypedIterator<A>) emptyTypedIterator;
    } else {
      return new TypedIterator<A>(head, type);
    }
  }
  
  
  public static Object createList(Object... values){
    if (values.length == 0){
      return null;
      
    } else if (values.length == 1){
      return values[0];
      
    } else {
      Node node = null, next = null;

      for (int i=values.length-1; i>=0; i--){
        node = new Node(values[i], next);
        next = node;
      }
      return node;
    }
  }
    
  public static Object valueOf (Object o){
    return (o instanceof Node) ? ((Node)o).data : o;
  }
    
  public static Object set (Object head, Object newHead){
    if (head == null || newHead instanceof SystemAttribute){
      return newHead; 
      
    } else {
      if (head instanceof Node){

        for (Node n = (Node)head; n != null; n = n.next){
          if (n.data instanceof SystemAttribute){
            throw new JPFException("attempt to overwrite SystemAttribute with " + newHead);
          }
        }
        
        return newHead; 
        
      } else { 
        if (head instanceof SystemAttribute){
          throw new JPFException("attempt to overwrite SystemAttribute with " + newHead);
        } else {
          return newHead; 
        }
      }
    }
  }
  
  
  public static Object forceSet (Object head, Object newHead){
    return newHead;
  }
  
  
  public static Object add (Object head, Object data){
    if (head == null){
      return data;
      
    } else if (data == null){
      return head;
      
    } else {
      if (head instanceof Node){
        return new Node(data, (Node)head);
        
      } else { 
        Node p = new Node(head,null);
        return new Node(data, p);
      }
    }
  }
  
  public static Object replace (Object head, Object oldData, Object newData){
    if (oldData == null){
      return head;
    }
    if (newData == null){
      return remove(head, oldData); 
    }
    
    if (head instanceof Node){

      return ((Node)head).cloneWithReplacedData(oldData, newData);
      
    } else { 
      if (oldData.equals(head)){
        return newData;
      } else {
        return head;
      }
    }
  }
  
  public static Object remove (Object head, Object data){
    if (head == null || data == null){
      return head;  
    }

    if (head instanceof Node) {
      Node nh = (Node) head;
      
      Node nhn = nh.next;
      if (nhn != null && nhn.next == null) { 
        if (nh.data.equals(data)) {
          return nhn.data;
        } else if (nhn.data.equals(data)) {
          return nh.data;
        } else { 
          return head;
        }
      }
      
      return nh.cloneWithRemovedData(data);
      
    } else { 
      if (head.equals(data)){
        return null;
      } else {
        return head;
      }
    }
  }
  
  public static boolean contains (Object head, Object o){
    if (head == null || o == null){
      return false;
      
    } else if (head instanceof Node){
      for (Node n = (Node)head; n != null; n = n.next){
        if (o.equals(n.data)){
          return true;
        }
      }
      return false;
            
    } else {
      return o.equals(head);
    }
  }
  
  public static boolean containsType (Object head, Class<?> type){
    if (head == null || type == null){
      return false;
      
    } else if (head instanceof Node){
      for (Node n = (Node)head; n != null; n = n.next){
        if (type.isAssignableFrom(n.data.getClass())){
          return true;
        }
      }
      return false;
            
    } else {
      return type.isAssignableFrom(head.getClass());
    }
  }
  


  public static boolean isList (Object head){
    return (head instanceof Node);
  }
  
  public static boolean isEmpty(Object head){
    return head == null;
  }
  
  public static int size(Object head){
    int len = 0;
    
    if (head instanceof Node){
      for (Node n = (Node) head; n != null; n = n.next) {
        len++;
      }    
    } else {
      if (head != null){
        len = 1;
      }
    }
    
    return len;
  }
  
  public static int numberOfInstances (Object head, Class<?> type){
    int len = 0;
    
    if (head instanceof Node){
      for (Node n = (Node) head; n != null; n = n.next) {
        if (type.isInstance(n.data)){
          len++;
        }
      }    
    } else {
      if (head != null){
        if (type.isInstance(head)){
          len = 1;
        }
      }
    }
    
    return len;
    
  }
  
  public static Object get (Object head, int idx){
    if (head instanceof Node){
      int i=0;
      for (Node n = (Node) head; n != null; n = n.next) {
        if (i++ == idx){
          return n.data;
        }
      }    
    } else {
      if (idx == 0){
        return head;
      }
    }
    
    return null;
  }
  
  public static Object getFirst(Object head){
    if (head instanceof Node){
      return ((Node)head).data;
    } else {
      return head;
    }
  }
  
  public static Object getNext(Object head, Object prevData){
    if (head instanceof Node){
      Node n = (Node)head;
      if (prevData != null){
        for (; n != null && n.data != prevData; n=n.next);
        if (n == null){
          return null;
        } else {
          n = n.next;
        }
      }
      
      return n.data;
      
    } else {
      if (prevData == null){
        return head;
      }
    }
    
    return null;    
  }
  
  public static <A> A getFirst (Object head, Class<A> type){
    if (head != null){
      if (type.isAssignableFrom(head.getClass())) {
        return (A) head;
      }

      if (head instanceof Node) {
        for (Node n = (Node) head; n != null; n = n.next) {
          if (type.isAssignableFrom(n.data.getClass())) {
            return (A) n.data;
          }
        }
      }
    }
    
    return null;
  }
  
  public static <A> A getNext (Object head, Class<A> type, Object prevData){
    if (head instanceof Node){
      Node n = (Node)head;
      if (prevData != null){
        for (; n != null && n.data != prevData; n=n.next);
        if (n == null){
          return null;
        } else {
          n = n.next;
        }
      }
      
      for (; n != null; n = n.next) {
        if (type.isAssignableFrom(n.data.getClass())) {
          return (A) n.data;
        }
      }
      
    } else if (head != null) {
      if (prevData == null){
        if (type.isAssignableFrom(head.getClass())){
          return (A)head;
        }
      }
    }
    
    return null;
  }
  
  public static void hash (Object head, HashData hd){
    if (head instanceof Node){
      for (Node n = (Node) head; n != null; n = n.next) {
        hd.add(n.data);
      }
            
    } else if (head != null){
      hd.add(head);
    }    
  }
  
  public static boolean equals( Object head1, Object head2){
    if (head1 != null){
      return head1.equals(head2);
    } else {
      return head2 == null; 
    }
  }
  
  static Object cloneData (Object o) throws CloneNotSupportedException {
    if (o instanceof CloneableObject) {
      CloneableObject co = (CloneableObject) o;
      return co.clone();
      
    } else if (o != null) {
      Class<?> cls = o.getClass();
      try {
        Method m = cls.getMethod("clone");


        return m.invoke(o);
        
      } catch (NoSuchMethodException nsmx){



        throw new CloneNotSupportedException("no public clone(): " + o);
      } catch (InvocationTargetException ix){
        throw new RuntimeException( "generic clone failed: " + o, ix.getCause());
      } catch (IllegalAccessException iax){
        throw new RuntimeException("clone() not accessible: " + o);
      }
      
    } else {
      return null;
    }
  }
  
  static Node cloneNode (Node n) throws CloneNotSupportedException {
    if (n == null){
      return null;
    } else {
      return new Node( cloneData(n.data), cloneNode(n.next));
    }
  }
    
  public static Object clone (Object head) throws CloneNotSupportedException {
    if (head instanceof Node){
      return cloneNode( (Node)head);
            
    } else if (head != null){
      return cloneData( head);
      
    } else {
      return null;
    }
    
  }
}
