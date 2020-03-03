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



package gov.nasa.jpf.util.script;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.DynamicObjectArray;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.SystemState;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;


public abstract class EventGeneratorFactory extends ListenerAdapter
                                         implements ElementProcessor, Iterable<EventGenerator> {

  static final String DEFAULT = "default";




  static class Memento {
    ArrayList<EventGenerator> queue;
    int cur; 

    Memento (EventGeneratorFactory fact) {
      queue = fact.queue;
      cur = fact.cur;
    }

    void restore (EventGeneratorFactory fact) {
      fact.queue = queue;
      fact.cur = cur;
    }
  }








  static class Loop extends EventGenerator {

    int startPos, endPos;

    Loop (String id, int startPos,  int endPos){
      super(id);
      
      this.startPos = startPos;
      this.endPos = endPos;
    }

    int getStartPos() {
      return startPos;
    }


    @Override
	public void advance() {}

    @Override
	public Class getChoiceType() {
      return null;
    }

    @Override
	public Object getNextChoice() {
      return null;
    }

    @Override
	public int getProcessedNumberOfChoices() {
      return 0;
    }

    @Override
	public int getTotalNumberOfChoices() {
      return 0;
    }

    @Override
	public boolean hasMoreChoices() {
      return false;
    }

    @Override
	public ChoiceGenerator randomize() {
      return null;
    }

    @Override
	public void reset() {}

  }

  
  protected int cur;

  
  DynamicObjectArray<Memento> states;

  protected String scriptFileName;
  protected Script script;
  protected Config conf;

  protected LinkedHashMap<String,ArrayList<EventGenerator>> sections;
  protected ArrayList<EventGenerator> queue;

  EventFactory efact;

  protected EventGeneratorFactory () {
    efact = null;
  }

  protected EventGeneratorFactory (EventFactory efact) {
    this.efact = efact;
  }

  protected void init (String fname) throws ESParser.Exception {
    cur = 0;
    states = new DynamicObjectArray<Memento>();

    sections = new LinkedHashMap<String,ArrayList<EventGenerator>>();
    queue = new ArrayList<EventGenerator>();
    sections.put(DEFAULT, queue);

    ESParser parser= new ESParser(fname, efact);
    script = parser.parse();
    scriptFileName = fname;

    script.process(this);
  }

  @Override
  public Iterator<EventGenerator> iterator() {
    return queue.iterator();
  }

  protected void addLoop (int startPos) {
    queue.add( new Loop( "loop", startPos, queue.size()-1));
  }

  public abstract Class<?> getEventType();

  
  public void reset () {
    cur = 0;
  }

  public String getScriptFileName() {
    return scriptFileName;
  }

  public Script getScript() {
    return script;
  }

  public boolean hasSection (String id) {
    return sections.containsKey(id);
  }

  public ArrayList<EventGenerator> getSection (String id) {
    return sections.get(id);
  }

  public ArrayList<EventGenerator> getDefaultSection () {
    return sections.get(DEFAULT);
  }

  protected void setQueue (ArrayList<EventGenerator> q) {
    if (queue != q) {
      queue = q;
      cur = 0;
    }
  }

  protected EventGenerator getNextEventGenerator() {
    EventGenerator cg;
    int n = queue.size();

    if (n == 0) {
      return null; 
    }

    if (cur < n) {
      cg = getQueueItem(cur); 


      if (cg instanceof Loop) {
        int tgtPos = ((Loop)cg).getStartPos();
        cg = queue.get(tgtPos);

        if (!cg.hasMoreChoices()) {
          for (int i=tgtPos; i<cur; i++) {
            queue.get(i).reset();
          }
        }

        cur = tgtPos;
      }

      cg.setId(Integer.toString(cur));






      cur++;
      return cg;

    } else {
      return null; 
    }
  }


  protected EventGenerator getQueueItem (int i) {
    return queue.get(i);
  }


  public int getTotalNumberOfEvents() {
    int total=0;
    int last = 1;

    for (EventGenerator cg : queue) {
       int level = cg.getTotalNumberOfChoices() * last;
       total += level;
       last = level;
    }

    return total;
  }

  public void printOn (PrintWriter pw) {
    for (EventGenerator eg : queue) {
      pw.println(eg);
    }
  }

  
  

  @Override
  public void searchStarted (Search search) {
    cur = 0;
  }

  @Override
  public void stateAdvanced (Search search) {
    int idx = search.getStateId();

    if (idx >= 0) { 
      Memento m = new Memento(this);
      states.set(idx, m);
    }
  }

  @Override
  public void stateBacktracked (Search search) {
    Memento m = states.get(search.getStateId());
    m.restore(this);

  }

  @Override
  public void stateRestored (Search search) {
    Memento m = states.get(search.getStateId());
    m.restore(this);


    SystemState ss = search.getVM().getSystemState();
    ChoiceGenerator cgNext = ss.getNextChoiceGenerator();
    cgNext.reset();
  }

}
