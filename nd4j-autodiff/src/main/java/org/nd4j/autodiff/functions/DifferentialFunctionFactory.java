package org.nd4j.autodiff.functions;

import java.util.List;

import com.kitfox.svg.A;
import org.nd4j.autodiff.AbstractFactory;
import org.nd4j.autodiff.ArrayField;
import org.nd4j.autodiff.Field;
import org.nd4j.autodiff.graph.Graph;
import org.nd4j.autodiff.opstate.NDArrayInformation;
import org.nd4j.autodiff.opstate.OpState;
import org.nd4j.linalg.api.ops.impl.accum.*;
import org.nd4j.linalg.api.ops.impl.transforms.*;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.util.ArrayUtil;

/**
 *
 * @param <X>
 */
public class DifferentialFunctionFactory<X extends Field<X>> implements FunctionFactory<X> {

    protected AbstractFactory<X> mFactory;
    protected Graph<NDArrayInformation,OpState> graph;

    public DifferentialFunctionFactory(Graph<NDArrayInformation,OpState> graph,AbstractFactory<X> mFactory) {
        if (mFactory != null) {
            this.mFactory = mFactory;
            this.graph = graph;
        } else {
            throw new IllegalArgumentException("Input not null value.");
        }
    }

    @Override
    public Constant<X> val(X iX) {
        return new Constant<>(mFactory.graph(),iX, mFactory);
    }


    @Override
    public Variable<X> var(String iName, X iX, PreEvaluator<X> preEvaluator) {
        return new Variable<>(mFactory.graph(),iName, iX, mFactory, preEvaluator);
    }

    @Override
    public Variable<X> var(String iName, X iX) {
        return new Variable<>(mFactory.graph(),iName, iX, mFactory);
    }

    @Override
    public Zero<X> zero() {
        return new Zero<>(graph,mFactory);
    }

    @Override
    public One<X> one() {
        return new One<>(graph,mFactory);
    }

    @Override
    public DifferentialFunction<X> tile(DifferentialFunction<X> iX, int[] repeat) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.tile(arg().getValue(),repeat);
            }

            @Override
            public double getReal() {
                throw new UnsupportedOperationException();
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                throw new UnsupportedOperationException();
            }


            @Override
            public String functionName() {
                return "tile";
            }
        };
    }


    @Override
    public DifferentialFunction<X> valueArrayOf(DifferentialFunction<X> iX, int[] shape) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.valueArrayOf(arg().getValue(),shape);
            }

            @Override
            public double getReal() {
                throw new UnsupportedOperationException();
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                throw new UnsupportedOperationException();
            }


            @Override
            public String functionName() {
                return "full";
            }
        };
    }

    @Override
    public DifferentialFunction<X> sum(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.sum(arg().doGetValue(),dimensions);
            }

            @Override
            public String functionName() {
                return new org.nd4j.linalg.api.ops.impl.accum.Sum().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return doRepeat(this,i_v1,dimensions);
            }
        };
    }

    @Override
    public DifferentialFunction<X> prod(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.prod(arg().doGetValue(),dimensions);
            }


            @Override
            public String functionName() {
                return new org.nd4j.linalg.api.ops.impl.accum.Prod().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return doRepeat(this,i_v1,dimensions).div(one().mul(getInputLength(i_v1)));
            }
        };
    }

    @Override
    public DifferentialFunction<X> mean(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.mean(arg().doGetValue(),dimensions);
            }


            @Override
            public String functionName() {
                return new Mean().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return doRepeat(this,i_v1,dimensions).div(one().mul(getInputLength(i_v1)));
            }
        };
    }

    @Override
    public DifferentialFunction<X> std(DifferentialFunction<X> i_x,
                                       boolean biasCorrected,
                                       int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.std(arg().doGetValue(),
                        biasCorrected ,
                        dimensions);
            }

            @Override
            public String functionName() {
                return new StandardDeviation().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                int inputs = getInputLength(i_v1);
                DifferentialFunction<X> g =  doRepeat(this,i_v1,dimensions);
                return g.mul(arg().minus(mean(arg(),dimensions))).div(one().mul(inputs));
            }
        };
    }

    @Override
    public DifferentialFunction<X> variance(DifferentialFunction<X> i_x,
                                            boolean biasCorrected,
                                            int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.variance(arg().doGetValue(),
                        biasCorrected, dimensions);
            }


            @Override
            public String functionName() {
                return new Variance().name();
            }


            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                int inputs = getInputLength(i_v1);
                DifferentialFunction<X> g =  doRepeat(this,i_v1,dimensions);
                return one().mul(2).mul(g).mul(arg().minus(mean(arg(),dimensions))).div(one().mul(inputs));
            }
        };
    }

    @Override
    public DifferentialFunction<X> max(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.max(arg().doGetValue(),dimensions);
            }


            @Override
            public String functionName() {
                return new Max().name();
            }


            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return doGradChoose(this,i_v1,dimensions);
            }
        };
    }

    @Override
    public DifferentialFunction<X> min(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.min(arg().doGetValue(),dimensions);
            }

            @Override
            public String functionName() {
                return new Min().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return doGradChoose(this,i_v1,dimensions);
            }
        };
    }

    @Override
    public DifferentialFunction<X> norm1(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.norm1(arg().doGetValue(),dimensions);
            }


            @Override
            public String functionName() {
                return new Norm1().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return null;
            }
        };
    }

    @Override
    public DifferentialFunction<X> norm2(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.norm2(arg().doGetValue(),dimensions);
            }


            @Override
            public String functionName() {
                return new Norm2().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return null;
            }
        };
    }

    @Override
    public DifferentialFunction<X> normmax(DifferentialFunction<X> i_x, int... dimensions) {
        return new AbstractReduceUnaryFunction<X>(graph,i_x,dimensions) {
            @Override
            protected X doGetValue() {
                return mFactory.normmax(arg().doGetValue(),dimensions);
            }

            @Override
            public String functionName() {
                return new NormMax().name();
            }



            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v1) {
                return null;
            }
        };
    }


    @Override
    public DifferentialFunction<X> abs(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.abs(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.abs(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return arg().div(abs(arg()));
            }


            @Override
            public String functionName() {
                return new Abs().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> cos(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.cos(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.cos(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return (sin(arg()).mul(arg().diff(i_v))).negate();
            }


            @Override
            public String functionName() {
                return new Cos().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> sin(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.sin(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.sin(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return cos(arg()).mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new Sin().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> tan(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.tan(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.tan(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return (new PolynomialTerm<>(mFactory.graph(),1, cos(arg()), -2)).mul(arg().diff(i_v));
            }



            @Override
            public String functionName() {
                return new Tan().name();
            }
        };
    }




    @Override
    public DifferentialFunction<X> transpose(DifferentialFunction<X> iX) {
        if(iX.getValue() instanceof ArrayField) {
            ArrayField arrayField = (ArrayField) iX.getValue();
            return new AbstractUnaryFunction<X>(mFactory.graph(),iX,ArrayUtil.reverseCopy(arrayField.getInput().getShape())) {

                @Override
                public X doGetValue() {
                    return mFactory.transpose(arg().getValue());
                }

                @Override
                public double getReal() {
                    return Math.tan(arg().getReal());
                }

                @Override
                public DifferentialFunction<X> diff(Variable<X> i_v) {
                    return this;
                }



                @Override
                public String functionName() {
                    return "transpose";
                }
            };
        }

        throw new IllegalStateException("Need the shape. This is only possible with ArrayField");
    }

    @Override
    public DifferentialFunction<X> acos(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.acos(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.acos(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(sqrt(one().minus(arg().pow(2)))).negate();
            }


            @Override
            public String functionName() {
                return new ACos().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> asin(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.asin(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.asin(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(sqrt(one().minus(arg().pow(2))));
            }



            @Override
            public String functionName() {
                return new ASin().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> atan(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.atan(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.atan(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(one().plus(arg().pow(2)));
            }

            @Override
            public String functionName() {
                return new ATan().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> cosh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.cosh(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.cosh(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return sinh(arg());
            }

            @Override
            public String functionName() {
                return new Cosh().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> sinh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.sinh(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.sinh(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return cosh(arg());
            }


            @Override
            public String functionName() {
                return new Sinh().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> tanh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.tanh(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.tanh(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(cosh(arg())).pow(2);
            }

            @Override
            public String functionName() {
                return new Tanh().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> acosh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.acosh(arg().getValue());
            }

            @Override
            public double getReal() {
                throw new IllegalStateException("");
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(sqrt(arg().minus(one())).mul(sqrt(arg().plus(one()))));
            }

            @Override
            public String functionName() {
                return new ACosh().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> asinh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.asinh(arg().getValue());
            }

            @Override
            public double getReal() {
                throw new IllegalStateException();
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(sqrt(arg().pow(2).plus(one())));
            }


            @Override
            public String functionName() {
                return new ASinh().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> atanh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.atanh(arg().getValue());
            }

            @Override
            public double getReal() {
                throw new IllegalStateException();
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().div(one().minus(arg().pow(2)));
            }


            @Override
            public String functionName() {
                return new ATanh().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> exp(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.exp(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.exp(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return exp(arg()).mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new Exp().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> log(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.log(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.log(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return new Inverse<>(graph,arg()).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new Log().name();
            }
        };
    }



    @Override
    public DifferentialFunction<X> or(DifferentialFunction<X> iX, DifferentialFunction<X> i_y) {
        return new AbstractBinaryFunction<X>(mFactory.graph(),iX, i_y) {

            @Override
            public X doGetValue() {
                return mFactory.or(larg().getValue(), rarg().getValue());
            }

            @Override
            public double getReal() {
                return Math.pow(larg().getReal(), rarg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                Constant<X> ym1 = DifferentialFunctionFactory.this
                        .val(rarg().getValue().minus(mFactory.one()));
                return rarg().mul(DifferentialFunctionFactory.this.pow(larg(), ym1))
                        .mul(larg().diff(i_v));
            }

            @Override
            public String toString() {
                return "or(" + larg().toString() + ", " + rarg().toString() + ")";
            }

            @Override
            public String doGetFormula(List<Variable<X>> variables) {
                return "or(" + larg().doGetFormula(variables) + ","
                        + rarg().doGetFormula(variables) + ")";
            }

            @Override
            public String functionName() {
                return new Or().name();
            }
        };
    }


    @Override
    public DifferentialFunction<X> eq(DifferentialFunction<X> iX, DifferentialFunction<X> i_y) {
        return new AbstractBinaryFunction<X>(mFactory.graph(),iX, i_y) {

            @Override
            public X doGetValue() {
                return mFactory.eq(larg().getValue(), rarg().getValue());
            }

            @Override
            public double getReal() {
                return Math.pow(larg().getReal(), rarg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                Constant<X> ym1 = DifferentialFunctionFactory.this
                        .val(rarg().getValue().minus(mFactory.one()));
                return rarg().mul(DifferentialFunctionFactory.this.pow(larg(), ym1))
                        .mul(larg().diff(i_v));
            }

            @Override
            public String toString() {
                return "eq(" + larg().toString() + ", " + rarg().toString() + ")";
            }

            @Override
            public String doGetFormula(List<Variable<X>> variables) {
                return "eq(" + larg().doGetFormula(variables) + ","
                        + rarg().doGetFormula(variables) + ")";
            }

            @Override
            public String functionName() {
                return new Not().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> neq(DifferentialFunction<X> iX, DifferentialFunction<X> i_y) {
        return new AbstractBinaryFunction<X>(mFactory.graph(),iX, i_y) {

            @Override
            public X doGetValue() {
                return mFactory.neq(larg().getValue(), rarg().getValue());
            }

            @Override
            public double getReal() {
                return Math.pow(larg().getReal(), rarg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                Constant<X> ym1 = DifferentialFunctionFactory.this
                        .val(rarg().getValue().minus(mFactory.one()));
                return rarg().mul(DifferentialFunctionFactory.this.pow(larg(), ym1))
                        .mul(larg().diff(i_v));
            }

            @Override
            public String toString() {
                return "neq(" + larg().toString() + ", " + rarg().toString() + ")";
            }

            @Override
            public String doGetFormula(List<Variable<X>> variables) {
                return "neq(" + larg().doGetFormula(variables) + ","
                        + rarg().doGetFormula(variables) + ")";
            }

            @Override
            public String functionName() {
                return new Not().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> pow(DifferentialFunction<X> iX, Constant<X> i_y) {
        return new AbstractBinaryFunction<X>(mFactory.graph(),iX, i_y) {

            @Override
            public X doGetValue() {
                return mFactory.pow(larg().getValue(), rarg().getValue());
            }

            @Override
            public double getReal() {
                return Math.pow(larg().getReal(), rarg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                Constant<X> ym1 = DifferentialFunctionFactory.this
                        .val(rarg().getValue().minus(mFactory.one()));
                return rarg().mul(DifferentialFunctionFactory.this.pow(larg(), ym1))
                        .mul(larg().diff(i_v));
            }

            @Override
            public String toString() {
                return "pow(" + larg().toString() + ", " + rarg().toString() + ")";
            }

            @Override
            public String doGetFormula(List<Variable<X>> variables) {
                return "pow(" + larg().doGetFormula(variables) + ","
                        + rarg().doGetFormula(variables) + ")";
            }

            @Override
            public String functionName() {
                return new Pow().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> sqrt(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.sqrt(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.sqrt(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return ((sqrt(arg()).inverse())
                        .div(val(mFactory.one().mul(2L))))
                        .mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new Sqrt().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> square(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.square(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.pow(arg().getReal(), 2);
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return arg().mul(val(mFactory.one().mul(2L)))
                        .mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new Pow().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> floor(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.floor(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                throw new RuntimeException("not allowed");
            }

            @Override
            public String functionName() {
                return new Floor().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> relu(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.relu(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return val(mFactory.step(arg().getValue())).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new RectifedLinear().name();
            }
        };
    }



    @Override
    public DifferentialFunction<X> softmax(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.softmax(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                DifferentialFunction<X> val = val(getValue());
                return val.mul(one().minus(val)).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new SoftMax().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> hardTanh(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.hardTanh(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return hardTanhDerivative(val(getValue())).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new HardTanh().name();
            }
        };
    }



    @Override
    public DifferentialFunction<X> hardTanhDerivative(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.hardTanhDerivative(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new HardTanhDerivative().name();
            }
        };
    }




    @Override
    public DifferentialFunction<X> sigmoid(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.sigmoid(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return sigmoidDerivative(arg()).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new Sigmoid().name();
            }
        };
    }



    @Override
    public DifferentialFunction<X> sigmoidDerivative(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.sigmoidDerivative(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return one().mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new SigmoidDerivative().name();
            }
        };
    }


    @Override
    public DifferentialFunction<X> sign(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.sign(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return zero();
            }

            @Override
            public String functionName() {
                return new Sign().name();
            }
        };
    }


    @Override
    public DifferentialFunction<X> broadcast(DifferentialFunction<X> iX, int... shape) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.broadcast(arg().getValue(),shape);
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String functionName() {
                return "broadcast";
            }
        };
    }

    private int getInputLength(Variable<X> func) {
        if(func.getValue() instanceof ArrayField) {
            ArrayField arrayField = (ArrayField) func.getValue();
            int[] inputShape = arrayField.getInput().getShape();
            return ArrayUtil.prod(inputShape);
        }

        throw new IllegalStateException("Only able to compute on array field");
    }

    private DifferentialFunction<X> doGradChoose(DifferentialFunction<X> func,Variable<X> input,int...axes) {
        if(input.getValue() instanceof ArrayField) {
            ArrayField arrayField = (ArrayField) input.getValue();
            DifferentialFunction<X> repeatedGrad = doRepeat(func,input,axes);
            DifferentialFunction<X> resultRepeated = doRepeat(func.args()[0],input,axes);
            DifferentialFunction<X> argMaxLocations = eq(input,resultRepeated);
            return argMaxLocations.mul(repeatedGrad).div(sum(argMaxLocations,axes));
        }

        throw new UnsupportedOperationException("Must be an ArrayField argument");

    }


    private DifferentialFunction<X> doRepeat(DifferentialFunction<X> func,
                                             Variable<X> input,
                                             int...axes) {
        if(input.getValue() instanceof ArrayField) {
            ArrayField arrayField = (ArrayField) input.getValue();
            int[] inputShape = arrayField.getInput().getShape();
            if(Shape.isWholeArray(inputShape,axes)) {
                return valueArrayOf(input,inputShape);
            }

            for(int i = 0; i < inputShape.length; i++) {
                inputShape[axes[i]] = 1;
            }

            return broadcast(func,inputShape);

        }

        throw new UnsupportedOperationException("Must be an ArrayField argument");

    }

    @Override
    public DifferentialFunction<X> repeat(DifferentialFunction<X> iX, int axis) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.repeat(arg().getValue(),axis);
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String functionName() {
                return "repeat";
            }
        };
    }

    @Override
    public DifferentialFunction<X> softsign(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.softsign(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return softsignDerivative(arg()).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new SoftSign().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> softsignDerivative(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.softsignDeriviative(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return zero();
            }


            @Override
            public String functionName() {
                return new SoftSignDerivative().name();
            }
        };
    }





    @Override
    public DifferentialFunction<X> softplus(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.softplus(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return sigmoid(arg()).mul(arg().diff(i_v));
            }

            @Override
            public String functionName() {
                return new SoftPlus().name();
            }
        };
    }


    @Override
    public DifferentialFunction<X> elu(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.elu(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return eluDerivative(arg()).mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new ELU().name();
            }
        };
    }



    @Override
    public DifferentialFunction<X> eluDerivative(DifferentialFunction<X> iX) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.eluDerivative(arg().getValue());
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return zero();
            }

            @Override
            public String functionName() {
                return new ELUDerivative().name();
            }
        };
    }




    @Override
    public DifferentialFunction<X> leakyRelu(DifferentialFunction<X> iX, double cutoff) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.leakyRelu(arg().getValue(),cutoff);
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return leakyReluDerivative(arg(),cutoff).mul(arg().diff(i_v));
            }


            @Override
            public String functionName() {
                return new LeakyReLU().name();
            }
        };
    }



    @Override
    public DifferentialFunction<X> leakyReluDerivative(DifferentialFunction<X> iX, double cutoff) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX) {

            @Override
            public X doGetValue() {
                return mFactory.leakyReluDerivative(arg().getValue(),cutoff);
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return zero();
            }

            @Override
            public String functionName() {
                return new LeakyReLUDerivative().name();
            }
        };
    }

    @Override
    public DifferentialFunction<X> reshape(Variable<X> iX, int[] shape) {
        return new AbstractUnaryFunction<X>(mFactory.graph(),iX,shape) {

            @Override
            public X doGetValue() {
                return mFactory.reshape(arg().getValue(),shape);
            }

            @Override
            public double getReal() {
                return Math.floor(arg().getReal());
            }

            @Override
            public DifferentialFunction<X> diff(Variable<X> i_v) {
                return this;
            }

            @Override
            public String functionName() {
                return "reshape";
            }
        };
    }
}