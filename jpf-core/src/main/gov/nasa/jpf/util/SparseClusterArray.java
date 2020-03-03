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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class SparseClusterArray <E> implements Iterable<E> {

  public static final int CHUNK_BITS = 8;
  public static final int CHUNK_SIZE = 256;
  public static final int N_ELEM = 1 << CHUNK_BITS;     
  protected static final int ELEM_MASK = 0xff;
  protected static final int BM_ENTRIES = N_ELEM / 64;     
  protected static final int MAX_BM_INDEX = BM_ENTRIES-1;



  public static final int SEG_BITS = 8;
  public static final int N_SEG = 1 << SEG_BITS;
  protected static final int SEG_MASK = 0xff;
  public static final int S1 = 32-SEG_BITS; 
  public static final int S2 = S1-SEG_BITS; 
  public static final int S3 = S2-SEG_BITS; 
  protected static final int CHUNK_BASEMASK = ~SEG_MASK;

  public static final int MAX_CLUSTERS = CHUNK_SIZE;      
  public static final int MAX_CLUSTER_ENTRIES = 0xffffff; 

  protected Root root;
  protected Chunk lastChunk;
  protected Chunk head;   
  protected int   nSet; 

  protected boolean trackChanges = false;
  protected Entry changes; 


  public static class Snapshot<T,E> {
    Object[] values;
    int[] indices;

    public Snapshot (int size){
      values = new Object[size];
      indices = new int[size];
    }

    public int size() {
      return indices.length;
    }
    public T getValue(int i){
      return (T) values[i];
    }
    public int getIndex(int i){
      return indices[i];
    }
  }


  public static class Entry<E> {  
    int index;
    E value;

    Entry<E> next;

    Entry (int index, E value){
      this.index = index;
      this.value = value;
    }
  }




  protected static class Root {
    public Node[] seg = new Node[N_SEG];
  }

  
  protected static class Node  {
    public ChunkNode[] seg = new ChunkNode[N_SEG];

  }

  protected static class ChunkNode  {
    public Chunk[] seg  = new Chunk[N_SEG];

  }

  protected static class Chunk implements Cloneable { 
    public int base, top;
    public Chunk next;
    public Object[] elements;  
    public long[] bitmap;



    protected Chunk() {}

    protected Chunk(int base){
      this.base = base;
      this.top = base + N_ELEM;

      elements = new Object[N_ELEM];
      bitmap = new long[BM_ENTRIES];
    }

    @Override
	public String toString() {
      return "Chunk [base=" + base + ",top=" + top + ']';
    }

    @SuppressWarnings("unchecked")
    public <E> Chunk deepCopy( Cloner<E> cloner) throws CloneNotSupportedException {
      Chunk nc = (Chunk) super.clone();

      E[] elem = (E[])elements;   
      Object[] e = new Object[N_ELEM];

      for (int i=nextSetBit(0); i>=0; i=nextSetBit(i+1)) {
        e[i] = cloner.clone(elem[i]);
      }

      nc.elements = e;
      nc.bitmap = bitmap.clone();

      return nc;
    }

    protected int nextSetBit (int iStart) {
      if (iStart < CHUNK_SIZE){
        long[] bm = bitmap;
        int j = (iStart >> 6); 
        long l = bm[j] & (0xffffffffffffffffL << iStart);

        while (true) {
          if (l != 0) {
            return Long.numberOfTrailingZeros(l) + (j << 6);
          } else {
            if (++j < BM_ENTRIES) {
              l = bm[j];
            } else {
              return -1;
            }
          }
        }
      } else {
        return -1;
      }
    }

    protected int nextClearBit (int iStart) {
      if (iStart < CHUNK_SIZE){
        long[] bm = bitmap;
        int j = (iStart >> 6); 
        long l = ~bm[j] & (0xffffffffffffffffL << iStart);

        while (true) {
          if (l != 0) {
            return Long.numberOfTrailingZeros(l) + (j << 6);
          } else {
            if (++j < BM_ENTRIES) {
              l = ~bm[j];
            } else {
              return -1;
            }
          }
        }
      } else {
        return -1;
      }
    }


    public boolean isEmpty() {
      long[] bm = bitmap;

      for (int i=0; i<BM_ENTRIES; i++){
        if (bm[i] != 0) return false;
      }

      return true;
    }
  }



  protected class ElementIterator<T>  implements Iterator<T>, Iterable<T> {
    int idx;    
    Chunk cur;  

    public ElementIterator () {
      for (Chunk c = head; c != null; c = c.next){
        int i = c.nextSetBit(0);
        if (i>=0){
          cur = c;
          idx = i;
          return;
        }
      }
    }

    @Override
	public boolean hasNext() {
      return (cur != null);
    }

    @Override
	@SuppressWarnings("unchecked")
    public T next() {
      Chunk c = cur;
      int i = idx;

      if (i < 0 || c == null){
        throw new NoSuchElementException();
      }

      Object ret = c.elements[i];
      cur = null;

      while (c!=null){
        i = c.nextSetBit(i+1);
        if (i>= 0){
          idx = i;
          cur = c;

          if (ret == null){

            ret = c.elements[i];
            continue;
          } else {
            break;
          }
        } else {
          i = -1;
        }
        c = c.next;
      }

      if (ret == null){

        throw new ConcurrentModificationException();
      }
      return (T)ret;
    }

    @Override
	public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
	public Iterator<T> iterator() {
      return this;
    }
  }

  protected class ElementIndexIterator implements IndexIterator {
    int idx;
    Chunk cur;

    public ElementIndexIterator () {
      for (Chunk c = head; c != null; c = c.next){
        int i = c.nextSetBit(0);
        if (i>=0){
          cur = c;
          idx = i;
          return;
        }
      }
    }

    public ElementIndexIterator (int startIdx){

      Chunk c;
      int i;


      for (c=head; c!= null; c=c.next) {
        if (c.top > startIdx) {
          cur = c;
          break;
        }
      }

      if (c.base < startIdx){
        i = startIdx & ELEM_MASK;
      } else {
        i = 0;
      }

      for (; c != null; c = c.next){
        i = c.nextSetBit(i);
        if (i>=0){
          cur = c;
          idx = i;
          return;
        } else {
          i = 0;
        }
      }
    }


    @Override
	public int next () {
      Chunk c = cur;
      int i = idx;

      if (i < 0 || c == null){
        return -1;
      }

      int iRet = (c.elements[i] != null) ? c.base + i : -1;
      cur = null;

      while (c!=null){
        i = c.nextSetBit(i+1);
        if (i>= 0){
          idx = i;
          cur = c;

          if (iRet < 0){

            iRet = c.base + i;
            continue;
          } else {
            break;
          }
        } else {
          i = -1;
        }
        c = c.next;
      }

      if (iRet < 0){

        throw new ConcurrentModificationException();
      }
      return iRet;
    }

  }




  void sortInChunk (Chunk newChunk) {
    if (head == null) {
      head = newChunk;
    } else {
      int base = newChunk.base;
      if (base < head.base) {
        newChunk.next = head;
        head = newChunk;
      } else {
        Chunk cprev, c;
        for (cprev=head, c=cprev.next; c != null; cprev=c, c=c.next) {
          if (base < c.base) {
            newChunk.next = c;
            break;
          }
        }
        cprev.next = newChunk;
      }
    }
  }



  public SparseClusterArray (){
    root = new Root();
  }

  
  protected SparseClusterArray (SparseClusterArray base){
    root = base.root;
    nSet = base.nSet;
    head = base.head;
  }

  @SuppressWarnings("unchecked")
  public E get (int i) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;

    if (i < 0){
      throw new IndexOutOfBoundsException();
    }

    if (l3 != null && (l3.base == (i & CHUNK_BASEMASK))) {  
      return (E) l3.elements[i & ELEM_MASK];
    }

    int  j = i >>>  S1;
    if ((l1 = root.seg[j]) != null) {           
      j = (i >>> S2) & SEG_MASK;
      if ((l2 = l1.seg[j]) != null) {           
        j = (i >>> S3) & SEG_MASK;
        if ((l3 = l2.seg[j]) != null) {         

          lastChunk = l3;
          return  (E) l3.elements[i & ELEM_MASK];
        }
      }
    }

    lastChunk = null;
    return null;
  }


  public void set (int i, E e) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;
    int j;

    if (i < 0){
      throw new IndexOutOfBoundsException();
    }

    if (l3 == null || (l3.base != (i & CHUNK_BASEMASK))) { 
      j = i >>>  S1;
      if ((l1 = root.seg[j]) == null) {         
        l1 = new Node();
        root.seg[j] = l1;

        j = (i >>> S2) & SEG_MASK;
        l2 = new ChunkNode();
        l1.seg[j] = l2;

        j = (i >>> S3) & SEG_MASK;
        l3 = new Chunk(i & ~ELEM_MASK);
        sortInChunk(l3);
        l2.seg[j] = l3;

      } else {                                  
        j = (i >>> S2) & SEG_MASK;
        if ((l2 = l1.seg[j]) == null) {         
          l2 = new ChunkNode();
          l1.seg[j] = l2;

          j = (i >>> S3) & SEG_MASK;
          l3 = new Chunk(i & ~ELEM_MASK);
          sortInChunk(l3);
          l2.seg[j] = l3;

        } else {                                
          j = (i >>> S3) & SEG_MASK;
          if ((l3 = l2.seg[j]) == null) {       
            l3 = new Chunk(i & ~ELEM_MASK);
            sortInChunk(l3);
            l2.seg[j] = l3;
          }
        }
      }

      lastChunk = l3;
    }

    j = i & ELEM_MASK;

    long[] bm = l3.bitmap;
    int u = (j >> 6);    
    int v = (i & 0x7f);  
    boolean isSet = ((bm[u] >> v) & 0x1) > 0;

    if (trackChanges) {
      Entry entry = new Entry(i,l3.elements[j]);
      entry.next = changes;
      changes = entry;
    }

    if (e != null) {
      if (!isSet) {
        l3.elements[j] = e;
        bm[u] |= (1L<<v);
        nSet++;
      }

    } else {
      if (isSet) {
        l3.elements[j] = null;
        bm[u] &= ~(1L<<v);
        nSet--;

      }
    }
  }

  
  public int firstNullIndex (int i, int length) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;
    int j;
    int iMax = i + length;

    if (l3 == null || (l3.base != (i & CHUNK_BASEMASK))) { 
      j = i >>>  S1;
      if ((l1 = root.seg[j]) != null) {         
        j = (i >>> S2) & SEG_MASK;
        if ((l2 = l1.seg[j]) != null) {         
          j = (i >>> S3) & SEG_MASK;
          if ((l3 = l2.seg[j]) == null){
            return i; 
          }
        } else {
          return i; 
        }
      } else { 
        return i;
      }
    }

    int k = i & SEG_MASK;
    while (l3 != null) {
      k = l3.nextClearBit(k);

      if (k >= 0) {             
        lastChunk = l3;
        i = l3.base + k;
        return (i < iMax) ? i : -1;

      } else {                  
        Chunk l3Next = l3.next;
        int nextBase = l3.base + CHUNK_SIZE;
        if ((l3Next != null) && (l3Next.base == nextBase)) {
          if (nextBase < iMax) {
            l3 = l3Next;
            k=0;
          } else {
            return -1;
          }
        } else {
          lastChunk = null;
          return (nextBase < iMax) ? nextBase : -1;
        }
      }
    }


    lastChunk = null;
    return i;
  }

  
  public SparseClusterArray<E> deepCopy (Cloner<E> elementCloner) {
    SparseClusterArray<E> a = new SparseClusterArray<E>();
    a.nSet = nSet;

    Node[] newNodeList = a.root.seg;

    Node newNode = null;
    ChunkNode newChunkNode = null;
    Chunk newChunk = null, lastChunk = null;

    Node[] nList = root.seg;

    try {
      for (int i=0, i1=0; i<nList.length; i++) {
        Node n = nList[i];
        if (n != null) {
          ChunkNode[] cnList = n.seg;

          for (int j=0, j1=0; j<cnList.length; j++) {
            ChunkNode cn = cnList[j];
            if (cn != null) {
              Chunk[] cList = cn.seg;

              for (int k=0, k1=0; k<cList.length; k++) {
                Chunk c = cList[k];

                if (c != null && !c.isEmpty()) {
                  newChunk = c.deepCopy(elementCloner);
                  if (lastChunk == null) {
                    a.head = lastChunk = newChunk;
                  } else {
                    lastChunk.next = newChunk;
                    lastChunk = newChunk;
                  }


                  if (newNode == null) {
                    newNode = new Node();
                    j1 = k1 = 0;
                    newNodeList[i1++] = newNode;
                  }

                  if (newChunkNode == null) {
                    newChunkNode = new ChunkNode();
                    newNode.seg[j1++] = newChunkNode;
                  }

                  newChunkNode.seg[k1++] = newChunk;
                }
              }
            }
            newChunkNode = null;
          }
        }
        newNode = null;
      }
    } catch (CloneNotSupportedException cnsx) {
      return null; 
    }

    return a;
  }

  
  public <T> Snapshot<E,T> getSnapshot (Transformer<E,T> transformer){
    Snapshot<E,T> snap = new Snapshot<E,T>(nSet);
    populateSnapshot(snap, transformer);

    return snap;
  }

  protected <T> void populateSnapshot (Snapshot<E,T> snap, Transformer<E,T> transformer){
    int n = nSet;

    Object[] values = snap.values;
    int[] indices = snap.indices;

    int j=0;
    for (Chunk c = head; c != null; c = c.next) {
      int base = c.base;
      int i=-1;
      while ((i=c.nextSetBit(i+1)) >= 0) {
        Object val = transformer.transform((E)c.elements[i]);
        values[j] = val;
        indices[j] = base + i;

        if (++j >= n) {
          break;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public <T> void restore (Snapshot<E,T> snap, Transformer<T,E> transformer) {


    clear();

    T[] values = (T[])snap.values;
    int[] indices = snap.indices;
    int len = indices.length;

    for (int i=0; i<len; i++){
      E obj = transformer.transform(values[i]);
      int index = indices[i];

      set(index,obj);
    }
  }

  public void clear() {
    lastChunk = null;
    head = null;
    root = new Root();
    nSet = 0;

    changes = null;
  }

  public void trackChanges () {
    trackChanges = true;
  }

  public void stopTrackingChanges() {
    trackChanges = false;
  }

  public boolean isTrackingChanges() {
    return trackChanges;
  }

  public Entry<E> getChanges() {
    return changes;
  }

  public void resetChanges() {
    changes = null;
  }

  public void revertChanges (Entry<E> changes) {
    for (Entry<E> e = changes; e != null; e = e.next) {
      set(e.index, e.value);
    }
  }

  @Override
  public String toString() {
    return "SparseClusterArray [nSet=" + nSet + ']';
  }

  public int numberOfElements() {
    return nSet;
  }
  
  public int numberOfChunks() {

    int n = 0;
    for (Chunk c = head; c != null; c = c.next) {
      n++;
    }
    return n;
  }



  public IndexIterator getElementIndexIterator () {
    return new ElementIndexIterator();
  }

  public IndexIterator getElementIndexIterator (int fromIndex) {
    return new ElementIndexIterator(fromIndex);
  }
  
  @Override
  public Iterator<E> iterator() {
    return new ElementIterator<E>();
  }

  public int cardinality () {
    return nSet;
  }
}
