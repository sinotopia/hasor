/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.compiler.ast.value;
import net.hasor.dataql.Option;
import net.hasor.dataql.compiler.ast.*;
import net.hasor.dataql.compiler.qil.CompilerStack;
import net.hasor.dataql.compiler.qil.InstQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 函数调用 - 之所以是 Variable 是由于 FunctionCall 的最终结果是 函数调用的返回值。而返回值是属于 Variable 的
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class FunCallRouteVariable implements RouteVariable {
    private RouteVariable  enterRoute;
    private List<Variable> paramList = new ArrayList<>();

    public FunCallRouteVariable(RouteVariable enterRoute) {
        this.enterRoute = enterRoute;
    }

    /** 添加入参 */
    public void addParam(Variable paramVar) {
        this.paramList.add(paramVar);
    }

    @Override
    public RouteVariable getParent() {
        return this.enterRoute;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        if (this.enterRoute != null) {
            this.enterRoute.accept(astVisitor);
        }
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                for (Variable param : paramList) {
                    param.accept(astVisitor);
                }
            }
        });
    }

    @Override
    public void doFormat(int depth, Option formatOption, FormatWriter writer) throws IOException {
        this.enterRoute.doFormat(depth, formatOption, writer);
        writer.write("(");
        for (int i = 0; i < this.paramList.size(); i++) {
            if (i > 0) {
                writer.write(", ");
            }
            this.paramList.get(i).doFormat(depth + 1, formatOption, writer);
        }
        writer.write(")");
    }

    @Override
    public void doCompiler(InstQueue queue, CompilerStack stackTree) {
        //
        int size = this.paramList.size();
        int methodAddress = 0;
        stackTree.newFrame();
        {
            // .输出 lambda 到一个新的函数中
            InstQueue instQueue = queue.newMethodInst();
            methodAddress = instQueue.getName();
            //
            // .函数定义
            instQueue.inst(METHOD, size);
            //
            // .声明函数参数的变量位置
            for (Variable name : this.paramList) {
                int index = stackTree.push("");//name);//将变量名压栈，并返回栈中的位置
                instQueue.inst(LOCAL, index, name);  //为栈中某个位置的变量命名
            }
            // .函数体
            //            super.doCompiler(instQueue, stackTree);
        }
        stackTree.dropFrame();
        //
        // .指向函数的指针
        queue.inst(M_REF, methodAddress);
    }
}