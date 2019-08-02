/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package qunar.tc.decompiler.modules.decompiler.exps;

import qunar.tc.decompiler.main.collectors.BytecodeMappingTracer;
import qunar.tc.decompiler.modules.decompiler.ExprProcessor;
import qunar.tc.decompiler.modules.decompiler.vars.CheckTypesResult;
import qunar.tc.decompiler.struct.gen.VarType;
import qunar.tc.decompiler.util.InterpreterUtil;
import qunar.tc.decompiler.util.TextBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArrayExprent extends Exprent {
    private Exprent array;
    private Exprent index;
    private final VarType hardType;

    public ArrayExprent(Exprent array, Exprent index, VarType hardType, Set<Integer> bytecodeOffsets) {
        super(EXPRENT_ARRAY);
        this.array = array;
        this.index = index;
        this.hardType = hardType;

        addBytecodeOffsets(bytecodeOffsets);
    }

    @Override
    public Exprent copy() {
        return new ArrayExprent(array.copy(), index.copy(), hardType, bytecode);
    }

    @Override
    public VarType getExprType() {
        VarType exprType = array.getExprType();
        if (exprType.equals(VarType.VARTYPE_NULL)) {
            return hardType.copy();
        } else {
            return exprType.decreaseArrayDim();
        }
    }

    public int getExprentUse() {
        return array.getExprentUse() & index.getExprentUse() & MULTIPLE_USES;
    }

    public CheckTypesResult checkExprTypeBounds() {
        CheckTypesResult result = new CheckTypesResult();
        result.addMinTypeExprent(index, VarType.VARTYPE_BYTECHAR);
        result.addMaxTypeExprent(index, VarType.VARTYPE_INT);
        return result;
    }

    public List<Exprent> getAllExprents() {
        List<Exprent> lst = new ArrayList<>();
        lst.add(array);
        lst.add(index);
        return lst;
    }

    @Override
    public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
        TextBuffer res = array.toJava(indent, tracer);

        if (array.getPrecedence() > getPrecedence()) { // array precedence equals 0
            res.enclose("(", ")");
        }

        VarType arrType = array.getExprType();
        if (arrType.arrayDim == 0) {
            VarType objArr = VarType.VARTYPE_OBJECT.resizeArrayDim(1); // type family does not change
            res.enclose("((" + ExprProcessor.getCastTypeName(objArr) + ")", ")");
        }

        tracer.addMapping(bytecode);

        return res.append('[').append(index.toJava(indent, tracer)).append(']');
    }

    @Override
    public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
        if (oldExpr == array) {
            array = newExpr;
        }
        if (oldExpr == index) {
            index = newExpr;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ArrayExprent)) return false;

        ArrayExprent arr = (ArrayExprent) o;
        return InterpreterUtil.equalObjects(array, arr.getArray()) &&
                InterpreterUtil.equalObjects(index, arr.getIndex());
    }

    public Exprent getArray() {
        return array;
    }

    public Exprent getIndex() {
        return index;
    }
}