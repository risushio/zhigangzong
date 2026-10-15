package com.zhigangzong.agent.extraction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** Missing/null model values preserve data; only explicit clear operations delete it. */
public final class StudentFieldPatch {
    private final ObjectNode values;
    private final Set<String> clearFields;
    StudentFieldPatch(ObjectNode values,Set<String> clearFields){this.values=values.deepCopy();this.clearFields=Set.copyOf(clearFields);}
    public ObjectNode toDraftPatch(){
        var patch=values.deepCopy();patch.remove("departmentName");
        if(patch.has("majorName")){patch.set("major",patch.get("majorName"));patch.remove("majorName");}
        clearFields.stream().filter(key->!key.equals("departmentName")).forEach(key->patch.putNull(key.equals("majorName")?"major":key));
        return patch;
    }
    public String departmentName(){return values.path("departmentName").asText(null);}
    public boolean clearsDepartment(){return clearFields.contains("departmentName");}
    public boolean isEmpty(){return values.isEmpty()&&clearFields.isEmpty();}
    @Override public String toString(){return "StudentFieldPatch[fields="+values.size()+", clears="+clearFields.size()+"]";}
}
