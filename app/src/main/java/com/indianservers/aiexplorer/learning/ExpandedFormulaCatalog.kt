package com.indianservers.aiexplorer.learning

internal data class ExpandedFormulaSeed(
    val category: FormulaCategory,
    val subcategory: String,
    val title: String,
    val expression: String,
    val variables: List<String>,
    val useCase: String,
    val level: KnowledgeLevel = KnowledgeLevel.School,
    val relatedTerms: List<String> = emptyList(),
)

private fun expandedFormula(
    category: FormulaCategory,
    subcategory: String,
    title: String,
    expression: String,
    variables: String,
    useCase: String,
    level: KnowledgeLevel = KnowledgeLevel.School,
    vararg relatedTerms: String,
) = ExpandedFormulaSeed(
    category = category,
    subcategory = subcategory,
    title = title,
    expression = expression,
    variables = variables.split(',').map(String::trim).filter(String::isNotBlank),
    useCase = useCase,
    level = level,
    relatedTerms = relatedTerms.toList(),
)

internal fun expandedFormulaCatalog(): List<ExpandedFormulaSeed> = buildList {
    val algebra = FormulaCategory.AlgebraFunctions
    add(expandedFormula(algebra, "Functions and Transformations", "Function composition", """\left(f\circ g\right)\left(x\right)=f\left(g\left(x\right)\right)""", "f,g,x", "Combine two functions in a specified order."))
    add(expandedFormula(algebra, "Functions and Transformations", "Inverse function identity", """f^{-1}\left(f\left(x\right)\right)=x""", "f,x", "Verify or use an inverse function."))
    add(expandedFormula(algebra, "Functions and Transformations", "Linear transformation", """f\left(x\right)=mx+c""", "f,x,m,c", "Model a constant rate with an initial value."))
    add(expandedFormula(algebra, "Exponents and Radicals", "Exponent product rule", """a^{m}a^{n}=a^{m+n}""", "a,m,n", "Simplify products with a common base."))
    add(expandedFormula(algebra, "Exponents and Radicals", "Exponent quotient rule", """\frac{a^{m}}{a^{n}}=a^{m-n}""", "a,m,n", "Simplify quotients with a common non-zero base."))
    add(expandedFormula(algebra, "Exponents and Radicals", "Rational exponent", """a^{\frac{m}{n}}=\sqrt[n]{a^{m}}""", "a,m,n", "Convert between radicals and fractional powers."))
    add(expandedFormula(algebra, "Inequalities", "Absolute value inequality", """\left|x-a\right|<r\Longleftrightarrow a-r<x<a+r""", "x,a,r", "Solve an interval described by distance from a center."))
    add(expandedFormula(algebra, "Inequalities", "Arithmetic geometric mean", """\frac{x_{1}+\cdots+x_{n}}{n}\geq\sqrt[n]{x_{1}\cdots x_{n}}""", "x,n", "Bound products or sums of non-negative values.", KnowledgeLevel.UG, "AM-GM"))
    add(expandedFormula(algebra, "Polynomials", "Factor theorem", """f\left(a\right)=0\Longleftrightarrow\left(x-a\right)\mid f\left(x\right)""", "f,a,x", "Test whether a linear expression is a polynomial factor.", KnowledgeLevel.UG))
    add(expandedFormula(algebra, "Polynomials", "Rational root candidates", """x=\pm\frac{\text{factor of }a_{0}}{\text{factor of }a_{n}}""", "x,a_0,a_n", "List possible rational zeroes of an integer polynomial.", KnowledgeLevel.UG))
    add(expandedFormula(algebra, "Sequences", "Infinite geometric series", """S_{\infty}=\frac{a_{1}}{1-r},\quad\left|r\right|<1""", "S,a_1,r", "Sum a convergent infinite geometric progression."))
    add(expandedFormula(algebra, "Sequences", "Harmonic sequence", """a_{n}=\frac{1}{a+\left(n-1\right)d}""", "a_n,a,n,d", "Describe reciprocals of an arithmetic progression."))

    val geometry = FormulaCategory.GeometryMensuration
    add(expandedFormula(geometry, "Plane Geometry", "Similar figure length scale", """\frac{L_{2}}{L_{1}}=k""", "L_1,L_2,k", "Relate corresponding lengths of similar plane figures."))
    add(expandedFormula(geometry, "Mensuration", "Similar figure area scale", """\frac{A_{2}}{A_{1}}=k^{2}""", "A_1,A_2,k", "Relate areas of similar figures using their linear scale factor."))
    add(expandedFormula(geometry, "Quadrilaterals", "Rectangle area", """A=l\,w""", "A,l,w", "Find the area enclosed by a rectangle."))
    add(expandedFormula(geometry, "Quadrilaterals", "Rectangle perimeter", """P=2\left(l+w\right)""", "P,l,w", "Find the boundary length of a rectangle."))
    add(expandedFormula(geometry, "Quadrilaterals", "Parallelogram area", """A=b\,h""", "A,b,h", "Find parallelogram area from base and perpendicular height."))
    add(expandedFormula(geometry, "Quadrilaterals", "Rhombus area", """A=\frac{1}{2}d_{1}d_{2}""", "A,d_1,d_2", "Find rhombus area from its diagonals."))
    add(expandedFormula(geometry, "Triangles", "Equilateral triangle area", """A=\frac{\sqrt{3}}{4}a^{2}""", "A,a", "Find the area of an equilateral triangle."))
    add(expandedFormula(geometry, "Triangles", "Triangle inradius area", """A=r\,s""", "A,r,s", "Relate triangle area, inradius, and semiperimeter."))
    add(expandedFormula(geometry, "Solid Geometry", "Cuboid volume", """V=l\,w\,h""", "V,l,w,h", "Find the capacity of a rectangular solid."))
    add(expandedFormula(geometry, "Solid Geometry", "Cuboid surface area", """S=2\left(lw+lh+wh\right)""", "S,l,w,h", "Find the total surface area of a cuboid."))
    add(expandedFormula(geometry, "Solid Geometry", "Prism volume", """V=A_{\mathrm{base}}h""", "V,A_base,h", "Find the volume of any uniform prism."))
    add(expandedFormula(geometry, "Solid Geometry", "Pyramid volume", """V=\frac{1}{3}A_{\mathrm{base}}h""", "V,A_base,h", "Find the volume of a pyramid."))
    add(expandedFormula(geometry, "Circles", "Annulus area", """A=\pi\left(R^{2}-r^{2}\right)""", "A,R,r", "Find the area between two concentric circles."))
    add(expandedFormula(geometry, "Polygons", "Regular polygon interior angle", """\alpha=\frac{\left(n-2\right)180^{\circ}}{n}""", "alpha,n", "Find one interior angle of a regular polygon."))

    val trig = FormulaCategory.Trigonometry
    add(expandedFormula(trig, "Reciprocal and Quotient Identities", "Reciprocal sine", """\csc\theta=\frac{1}{\sin\theta}""", "theta", "Replace cosecant with sine or conversely."))
    add(expandedFormula(trig, "Reciprocal and Quotient Identities", "Reciprocal cosine", """\sec\theta=\frac{1}{\cos\theta}""", "theta", "Replace secant with cosine or conversely."))
    add(expandedFormula(trig, "Reciprocal and Quotient Identities", "Cotangent quotient", """\cot\theta=\frac{\cos\theta}{\sin\theta}""", "theta", "Rewrite cotangent using sine and cosine."))
    add(expandedFormula(trig, "Angle Formulas", "Tangent addition", """\tan\left(A+B\right)=\frac{\tan A+\tan B}{1-\tan A\tan B}""", "A,B", "Expand the tangent of an angle sum."))
    add(expandedFormula(trig, "Angle Formulas", "Cosine subtraction", """\cos\left(A-B\right)=\cos A\cos B+\sin A\sin B""", "A,B", "Expand the cosine of an angle difference."))
    add(expandedFormula(trig, "Product and Sum Identities", "Cosine product to sum", """\cos A\cos B=\frac{1}{2}\left[\cos\left(A+B\right)+\cos\left(A-B\right)\right]""", "A,B", "Convert a cosine product into a sum.", KnowledgeLevel.UG))
    add(expandedFormula(trig, "Product and Sum Identities", "Cosine sum to product", """\cos A+\cos B=2\cos\frac{A+B}{2}\cos\frac{A-B}{2}""", "A,B", "Convert a cosine sum into a product.", KnowledgeLevel.UG))
    add(expandedFormula(trig, "Triangle Laws", "Triangle area with sine", """A=\frac{1}{2}ab\sin C""", "A,a,b,C", "Find triangle area from two sides and their included angle."))
    add(expandedFormula(trig, "Inverse and Hyperbolic", "Inverse sine relation", """\sin^{-1}x=\theta\Longleftrightarrow\sin\theta=x""", "x,theta", "Recover an angle from its sine value."))
    add(expandedFormula(trig, "Inverse and Hyperbolic", "Hyperbolic identity", """\cosh^{2}x-\sinh^{2}x=1""", "x", "Simplify expressions involving hyperbolic sine and cosine.", KnowledgeLevel.UG))
    add(expandedFormula(trig, "Applications", "Height by elevation", """h=d\tan\theta""", "h,d,theta", "Find an inaccessible height from horizontal distance and elevation angle."))
    add(expandedFormula(trig, "Radians", "Circular arc in radians", """s=r\theta""", "s,r,theta", "Find arc length when the central angle is in radians."))

    val calculus = FormulaCategory.CalculusAnalysis
    add(expandedFormula(calculus, "Standard Derivatives", "Exponential derivative", """\frac{d}{dx}e^{x}=e^{x}""", "x", "Differentiate the natural exponential function.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Standard Derivatives", "Logarithm derivative", """\frac{d}{dx}\ln x=\frac{1}{x}""", "x", "Differentiate the natural logarithm.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Standard Derivatives", "Sine derivative", """\frac{d}{dx}\sin x=\cos x""", "x", "Differentiate sine.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Standard Derivatives", "Tangent derivative", """\frac{d}{dx}\tan x=\sec^{2}x""", "x", "Differentiate tangent.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Standard Integrals", "Power integral", """\int x^{n}\,dx=\frac{x^{n+1}}{n+1}+C,\quad n\ne-1""", "x,n,C", "Integrate a power of the variable.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Standard Integrals", "Exponential integral", """\int e^{ax}\,dx=\frac{1}{a}e^{ax}+C""", "a,x,C", "Integrate an exponential with a linear exponent.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Standard Integrals", "Sine integral", """\int\sin x\,dx=-\cos x+C""", "x,C", "Integrate sine.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Series", "Maclaurin exponential series", """e^{x}=\sum_{n=0}^{\infty}\frac{x^{n}}{n!}""", "x,n", "Approximate the exponential near zero.", KnowledgeLevel.UG))
    add(expandedFormula(calculus, "Multivariable Calculus", "Jacobian determinant", """J=\det\left(\frac{\partial\left(u_{1},\ldots,u_{n}\right)}{\partial\left(x_{1},\ldots,x_{n}\right)}\right)""", "J,u,x,n", "Transform variables in multiple integrals.", KnowledgeLevel.PG))
    add(expandedFormula(calculus, "Multivariable Calculus", "Hessian matrix", """H_{ij}=\frac{\partial^{2}f}{\partial x_{i}\partial x_{j}}""", "H,i,j,f,x", "Analyze curvature and classify multivariable critical points.", KnowledgeLevel.PG))
    add(expandedFormula(calculus, "Vector Calculus", "Green theorem", """\oint_{C}P\,dx+Q\,dy=\iint_{D}\left(\frac{\partial Q}{\partial x}-\frac{\partial P}{\partial y}\right)\,dA""", "C,D,P,Q,x,y", "Convert a planar line integral into an area integral.", KnowledgeLevel.PG))
    add(expandedFormula(calculus, "Vector Calculus", "Divergence theorem", """\iiint_{V}\nabla\cdot\mathbf{F}\,dV=\iint_{\partial V}\mathbf{F}\cdot\mathbf{n}\,dS""", "V,F,n", "Relate flux through a closed surface to volume divergence.", KnowledgeLevel.PG))

    val ode = FormulaCategory.DifferentialEquations
    add(expandedFormula(ode, "First-order ODEs", "Bernoulli equation", """\frac{dy}{dx}+P\left(x\right)y=Q\left(x\right)y^{n}""", "x,y,P,Q,n", "Recognize a nonlinear first-order equation reducible to linear form.", KnowledgeLevel.UG))
    add(expandedFormula(ode, "First-order ODEs", "Exact differential condition", """\frac{\partial M}{\partial y}=\frac{\partial N}{\partial x}""", "M,N,x,y", "Test whether a first-order differential form is exact.", KnowledgeLevel.UG))
    add(expandedFormula(ode, "Second-order ODEs", "Damped oscillator", """m\frac{d^{2}x}{dt^{2}}+c\frac{dx}{dt}+kx=0""", "m,x,t,c,k", "Model free damped vibration.", KnowledgeLevel.UG))
    add(expandedFormula(ode, "Second-order ODEs", "Forced oscillator", """m\frac{d^{2}x}{dt^{2}}+c\frac{dx}{dt}+kx=F_{0}\cos\omega t""", "m,x,t,c,k,F_0,omega", "Model periodic forcing and resonance.", KnowledgeLevel.UG))
    add(expandedFormula(ode, "Systems and PDEs", "Linear system solution", """\mathbf{x}\left(t\right)=e^{At}\mathbf{x}_{0}""", "x,t,A,x_0", "Solve a constant-coefficient linear differential system.", KnowledgeLevel.PG))
    add(expandedFormula(ode, "Systems and PDEs", "Heat equation", """\frac{\partial u}{\partial t}=\alpha\nabla^{2}u""", "u,t,alpha", "Model diffusion of heat or concentration.", KnowledgeLevel.PG))
    add(expandedFormula(ode, "Systems and PDEs", "Wave equation", """\frac{\partial^{2}u}{\partial t^{2}}=c^{2}\nabla^{2}u""", "u,t,c", "Model propagating waves.", KnowledgeLevel.PG))
    add(expandedFormula(ode, "Systems and PDEs", "Laplace equation", """\nabla^{2}u=0""", "u", "Model steady potentials and equilibrium fields.", KnowledgeLevel.PG))
    add(expandedFormula(ode, "Laplace Methods", "Laplace transform", """F\left(s\right)=\int_{0}^{\infty}e^{-st}f\left(t\right)\,dt""", "F,s,t,f", "Transform an initial-value problem into algebraic form.", KnowledgeLevel.UG))
    add(expandedFormula(ode, "Laplace Methods", "Inverse Laplace transform", """f\left(t\right)=\mathcal{L}^{-1}\left\{F\left(s\right)\right\}""", "f,t,F,s", "Recover a time-domain solution from its transform.", KnowledgeLevel.UG))
    add(expandedFormula(ode, "Boundary Problems", "Sturm Liouville form", """-\frac{d}{dx}\left(p\frac{dy}{dx}\right)+qy=\lambda wy""", "p,y,x,q,lambda,w", "Represent an eigenvalue boundary-value problem.", KnowledgeLevel.PG))
    add(expandedFormula(ode, "Numerical ODEs", "Heun update", """y_{n+1}=y_{n}+\frac{h}{2}\left(k_{1}+k_{2}\right)""", "y,n,h,k_1,k_2", "Improve Euler approximation with a predictor-corrector step.", KnowledgeLevel.UG))

    val linear = FormulaCategory.LinearAlgebraVectors
    add(expandedFormula(linear, "Matrices", "Transpose product", """\left(AB\right)^{T}=B^{T}A^{T}""", "A,B", "Transpose a matrix product in the correct order.", KnowledgeLevel.UG))
    add(expandedFormula(linear, "Determinants", "Determinant product", """\det\left(AB\right)=\det\left(A\right)\det\left(B\right)""", "A,B", "Find or simplify the determinant of a product.", KnowledgeLevel.UG))
    add(expandedFormula(linear, "Systems", "Linear system", """A\mathbf{x}=\mathbf{b}""", "A,x,b", "Represent simultaneous linear equations compactly.", KnowledgeLevel.UG))
    add(expandedFormula(linear, "Systems", "Normal equations", """X^{T}X\hat{\beta}=X^{T}y""", "X,beta,y", "Solve an ordinary least-squares problem.", KnowledgeLevel.UG))
    add(expandedFormula(linear, "Eigenvalues", "Diagonalization", """A=PDP^{-1}""", "A,P,D", "Express a diagonalizable matrix using eigenvectors and eigenvalues.", KnowledgeLevel.PG))
    add(expandedFormula(linear, "Eigenvalues", "Spectral decomposition", """A=Q\Lambda Q^{T}""", "A,Q,Lambda", "Decompose a real symmetric matrix.", KnowledgeLevel.PG))
    add(expandedFormula(linear, "Decompositions", "LU decomposition", """A=L\,U""", "A,L,U", "Factor a matrix for efficient system solving.", KnowledgeLevel.UG))
    add(expandedFormula(linear, "Decompositions", "Cholesky decomposition", """A=LL^{T}""", "A,L", "Factor a positive-definite matrix.", KnowledgeLevel.PG))
    add(expandedFormula(linear, "Vector Products", "Cross product magnitude", """\left\lVert\mathbf{a}\times\mathbf{b}\right\rVert=\left\lVert\mathbf{a}\right\rVert\left\lVert\mathbf{b}\right\rVert\sin\theta""", "a,b,theta", "Relate cross product magnitude to angle and area.", KnowledgeLevel.UG))
    add(expandedFormula(linear, "Projections", "Orthogonal projection matrix", """P=A\left(A^{T}A\right)^{-1}A^{T}""", "P,A", "Project vectors onto the column space of a full-rank matrix.", KnowledgeLevel.PG))
    add(expandedFormula(linear, "Matrices", "Moore Penrose pseudoinverse", """A^{+}=V\Sigma^{+}U^{T}""", "A,U,Sigma,V", "Solve least-squares and minimum-norm systems.", KnowledgeLevel.PG))
    add(expandedFormula(linear, "Dot Products", "Cosine similarity", """\cos\theta=\frac{\mathbf{a}\cdot\mathbf{b}}{\left\lVert\mathbf{a}\right\rVert\left\lVert\mathbf{b}\right\rVert}""", "a,b,theta", "Compare vector directions independent of magnitude.", KnowledgeLevel.UG))

    val coordinate = FormulaCategory.CoordinateGeometry3D
    add(expandedFormula(coordinate, "Lines", "Section formula", """P=\left(\frac{mx_{2}+nx_{1}}{m+n},\frac{my_{2}+ny_{1}}{m+n}\right)""", "P,m,n,x_1,x_2,y_1,y_2", "Divide a line segment internally in a given ratio."))
    add(expandedFormula(coordinate, "Lines", "Angle between lines", """\tan\theta=\left|\frac{m_{2}-m_{1}}{1+m_{1}m_{2}}\right|""", "theta,m_1,m_2", "Find the acute angle between two lines."))
    add(expandedFormula(coordinate, "Conics", "General circle", """x^{2}+y^{2}+2gx+2fy+c=0""", "x,y,g,f,c", "Represent a circle in expanded coordinate form."))
    add(expandedFormula(coordinate, "Conics", "Parabola focus", """y^{2}=4ax,\quad F=\left(a,0\right)""", "x,y,a,F", "Connect a standard parabola to its focus."))
    add(expandedFormula(coordinate, "Conics", "Ellipse focal relation", """c^{2}=a^{2}-b^{2}""", "a,b,c", "Find the focal distance of an ellipse."))
    add(expandedFormula(coordinate, "Conics", "Hyperbola asymptotes", """y-k=\pm\frac{b}{a}\left(x-h\right)""", "x,y,h,k,a,b", "Find asymptotes of a horizontal hyperbola."))
    add(expandedFormula(coordinate, "Polar Coordinates", "Polar Cartesian conversion", """x=r\cos\theta,\quad y=r\sin\theta""", "x,y,r,theta", "Convert a polar point into Cartesian coordinates."))
    add(expandedFormula(coordinate, "Polar Coordinates", "Polar radius", """r^{2}=x^{2}+y^{2}""", "r,x,y", "Convert Cartesian coordinates to polar radius."))
    add(expandedFormula(coordinate, "Coordinate Solids", "Cylindrical coordinates", """x=r\cos\theta,\quad y=r\sin\theta,\quad z=z""", "x,y,z,r,theta", "Represent a 3D point using cylindrical coordinates.", KnowledgeLevel.UG))
    add(expandedFormula(coordinate, "Coordinate Solids", "Spherical coordinates", """x=\rho\sin\phi\cos\theta,\quad y=\rho\sin\phi\sin\theta,\quad z=\rho\cos\phi""", "x,y,z,rho,phi,theta", "Represent a 3D point using spherical coordinates.", KnowledgeLevel.UG))
    add(expandedFormula(coordinate, "Planes", "Angle line plane", """\sin\theta=\frac{\left|\mathbf{n}\cdot\mathbf{v}\right|}{\left\lVert\mathbf{n}\right\rVert\left\lVert\mathbf{v}\right\rVert}""", "theta,n,v", "Find the acute angle between a line and a plane.", KnowledgeLevel.UG))
    add(expandedFormula(coordinate, "3D Distance", "Skew line distance", """d=\frac{\left|\left(\mathbf{r}_{2}-\mathbf{r}_{1}\right)\cdot\left(\mathbf{v}_{1}\times\mathbf{v}_{2}\right)\right|}{\left\lVert\mathbf{v}_{1}\times\mathbf{v}_{2}\right\rVert}""", "d,r_1,r_2,v_1,v_2", "Find the shortest distance between skew lines.", KnowledgeLevel.PG))

    val probability = FormulaCategory.ProbabilityCombinatorics
    add(expandedFormula(probability, "Random Variables", "Continuous expected value", """E\left(X\right)=\int_{-\infty}^{\infty}xf_{X}\left(x\right)\,dx""", "X,x,f", "Find the mean of a continuous random variable.", KnowledgeLevel.UG))
    add(expandedFormula(probability, "Random Variables", "Variance of sum", """\operatorname{Var}\left(X+Y\right)=\operatorname{Var}\left(X\right)+\operatorname{Var}\left(Y\right)+2\operatorname{Cov}\left(X,Y\right)""", "X,Y", "Find the spread of a sum of dependent random variables.", KnowledgeLevel.UG))
    add(expandedFormula(probability, "Random Variables", "Law of total variance", """\operatorname{Var}\left(X\right)=E\left[\operatorname{Var}\left(X\mid Y\right)\right]+\operatorname{Var}\left(E\left[X\mid Y\right]\right)""", "X,Y", "Decompose variability into within- and between-group parts.", KnowledgeLevel.PG))
    add(expandedFormula(probability, "Probability Rules", "Mutual exclusivity", """A\cap B=\varnothing\Rightarrow P\left(A\cup B\right)=P\left(A\right)+P\left(B\right)""", "A,B", "Add probabilities of disjoint events."))
    add(expandedFormula(probability, "Counting", "Circular permutations", """N=\left(n-1\right)!""", "N,n", "Count arrangements around a circle."))
    add(expandedFormula(probability, "Counting", "Combinations with repetition", """N=\binom{n+r-1}{r}""", "N,n,r", "Count unordered selections when repetition is allowed."))
    add(expandedFormula(probability, "Generating Functions", "Probability generating function", """G_{X}\left(s\right)=E\left(s^{X}\right)""", "G,X,s", "Encode a non-negative integer distribution.", KnowledgeLevel.PG))
    add(expandedFormula(probability, "Generating Functions", "Binomial generating function", """G_{X}\left(s\right)=\left(1-p+ps\right)^{n}""", "G,X,s,p,n", "Analyze a binomial distribution using its generating function.", KnowledgeLevel.PG))
    add(expandedFormula(probability, "Stochastic Processes", "Markov transition update", """\boldsymbol{\pi}_{n+1}=\boldsymbol{\pi}_{n}P""", "pi,n,P", "Advance a finite Markov chain by one step.", KnowledgeLevel.UG))
    add(expandedFormula(probability, "Stochastic Processes", "Poisson process count", """P\left(N\left(t\right)=k\right)=e^{-\lambda t}\frac{\left(\lambda t\right)^{k}}{k!}""", "N,t,k,lambda", "Find event-count probabilities in a Poisson process.", KnowledgeLevel.UG))
    add(expandedFormula(probability, "Graph Counting", "Complete graph edges", """\left|E\left(K_{n}\right)\right|=\frac{n\left(n-1\right)}{2}""", "E,K,n", "Count edges in a complete simple graph."))
    add(expandedFormula(probability, "Graph Counting", "Tree edge count", """\left|E\right|=\left|V\right|-1""", "E,V", "Relate vertices and edges in a finite tree."))

    val statistics = FormulaCategory.StatisticsDistributions
    add(expandedFormula(statistics, "Descriptive Statistics", "Weighted mean", """\bar{x}_{w}=\frac{\sum_{i=1}^{n}w_{i}x_{i}}{\sum_{i=1}^{n}w_{i}}""", "x,w,i,n", "Average values with unequal importance."))
    add(expandedFormula(statistics, "Descriptive Statistics", "Grouped data median", """\operatorname{Median}=l+\frac{\frac{N}{2}-F}{f}h""", "l,N,F,f,h", "Estimate the median from grouped frequencies."))
    add(expandedFormula(statistics, "Descriptive Statistics", "Grouped data mode", """\operatorname{Mode}=l+\frac{f_{1}-f_{0}}{2f_{1}-f_{0}-f_{2}}h""", "l,f_0,f_1,f_2,h", "Estimate the mode from grouped frequencies."))
    add(expandedFormula(statistics, "Sampling", "Sample proportion", """\hat{p}=\frac{x}{n}""", "p,x,n", "Estimate a population proportion from a sample."))
    add(expandedFormula(statistics, "Sampling", "Proportion standard error", """SE_{\hat{p}}=\sqrt{\frac{\hat{p}\left(1-\hat{p}\right)}{n}}""", "SE,p,n", "Estimate sampling variability of a proportion."))
    add(expandedFormula(statistics, "Inference", "Proportion confidence interval", """\hat{p}\pm z_{\frac{\alpha}{2}}\sqrt{\frac{\hat{p}\left(1-\hat{p}\right)}{n}}""", "p,z,alpha,n", "Estimate a population proportion with uncertainty.", KnowledgeLevel.UG))
    add(expandedFormula(statistics, "Regression", "Coefficient of determination", """R^{2}=1-\frac{SS_{\mathrm{res}}}{SS_{\mathrm{tot}}}""", "R,SS_res,SS_tot", "Measure explained variation in a regression model.", KnowledgeLevel.UG))
    add(expandedFormula(statistics, "Nonparametric Methods", "Spearman rank correlation", """r_{s}=1-\frac{6\sum d_{i}^{2}}{n\left(n^{2}-1\right)}""", "r,d,i,n", "Measure monotonic association using ranks.", KnowledgeLevel.UG))
    add(expandedFormula(statistics, "Nonparametric Methods", "Mann Whitney statistic", """U_{1}=n_{1}n_{2}+\frac{n_{1}\left(n_{1}+1\right)}{2}-R_{1}""", "U,n_1,n_2,R_1", "Compare two independent samples using ranks.", KnowledgeLevel.UG))
    add(expandedFormula(statistics, "Time Series", "Simple moving average", """MA_{t}=\frac{1}{k}\sum_{i=0}^{k-1}x_{t-i}""", "MA,t,k,i,x", "Smooth a time series over a fixed window."))
    add(expandedFormula(statistics, "Time Series", "Autocorrelation", """\rho_{k}=\frac{\operatorname{Cov}\left(X_{t},X_{t-k}\right)}{\operatorname{Var}\left(X_{t}\right)}""", "rho,k,X,t", "Measure dependence between lagged observations.", KnowledgeLevel.PG))
    add(expandedFormula(statistics, "Continuous Distributions", "Normal CDF", """F\left(x\right)=\frac{1}{2}\left[1+\operatorname{erf}\left(\frac{x-\mu}{\sigma\sqrt{2}}\right)\right]""", "F,x,mu,sigma", "Find cumulative probability under a normal distribution.", KnowledgeLevel.UG))

    val number = FormulaCategory.NumberTheory
    add(expandedFormula(number, "Arithmetic Functions", "Euler totient product", """\varphi\left(n\right)=n\prod_{p\mid n}\left(1-\frac{1}{p}\right)""", "phi,n,p", "Count positive integers up to n that are coprime to n.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Modular Arithmetic", "Modular inverse condition", """ax\equiv1\pmod{m}\Longleftrightarrow\gcd\left(a,m\right)=1""", "a,x,m", "Determine when a modular inverse exists.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Diophantine Equations", "Linear Diophantine solvability", """ax+by=c\text{ has a solution }\Longleftrightarrow\gcd\left(a,b\right)\mid c""", "a,b,c,x,y", "Test whether an integer linear equation has solutions.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Diophantine Equations", "Pythagorean triples", """a=m^{2}-n^{2},\quad b=2mn,\quad c=m^{2}+n^{2}""", "a,b,c,m,n", "Generate primitive right-triangle integer triples."))
    add(expandedFormula(number, "Sequences", "Binet Fibonacci formula", """F_{n}=\frac{\varphi^{n}-\psi^{n}}{\sqrt{5}}""", "F,n,phi,psi", "Compute Fibonacci numbers in closed form.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Sequences", "Fibonacci recurrence", """F_{n}=F_{n-1}+F_{n-2}""", "F,n", "Generate Fibonacci numbers recursively."))
    add(expandedFormula(number, "Continued Fractions", "Simple continued fraction", """x=a_{0}+\frac{1}{a_{1}+\frac{1}{a_{2}+\cdots}}""", "x,a", "Represent a real number by successive integer parts.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Cryptography", "RSA encryption", """c\equiv m^{e}\pmod{n}""", "c,m,e,n", "Encrypt a message representative in RSA.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Cryptography", "RSA decryption", """m\equiv c^{d}\pmod{n}""", "m,c,d,n", "Recover an RSA message representative.", KnowledgeLevel.UG))
    add(expandedFormula(number, "Primes", "Mersenne number", """M_{p}=2^{p}-1""", "M,p", "Generate candidates for Mersenne primes."))
    add(expandedFormula(number, "Divisibility", "Number of trailing zeroes", """Z\left(n!\right)=\sum_{k=1}^{\infty}\left\lfloor\frac{n}{5^{k}}\right\rfloor""", "Z,n,k", "Count trailing zeroes in a factorial."))
    add(expandedFormula(number, "Modular Arithmetic", "Fermat modular inverse", """a^{-1}\equiv a^{p-2}\pmod{p}""", "a,p", "Find an inverse modulo a prime.", KnowledgeLevel.UG))

    val complex = FormulaCategory.ComplexNumbers
    add(expandedFormula(complex, "Algebraic Form", "Complex addition", """\left(a+b\mathrm{i}\right)+\left(c+d\mathrm{i}\right)=\left(a+c\right)+\left(b+d\right)\mathrm{i}""", "a,b,c,d", "Add complex numbers component-wise."))
    add(expandedFormula(complex, "Algebraic Form", "Complex multiplication", """\left(a+b\mathrm{i}\right)\left(c+d\mathrm{i}\right)=\left(ac-bd\right)+\left(ad+bc\right)\mathrm{i}""", "a,b,c,d", "Multiply complex numbers in Cartesian form."))
    add(expandedFormula(complex, "Polar Form", "Polar multiplication", """r_{1}e^{\mathrm{i}\theta_{1}}r_{2}e^{\mathrm{i}\theta_{2}}=r_{1}r_{2}e^{\mathrm{i}\left(\theta_{1}+\theta_{2}\right)}""", "r_1,r_2,theta_1,theta_2", "Multiply magnitudes and add arguments."))
    add(expandedFormula(complex, "Polar Form", "Polar division", """\frac{r_{1}e^{\mathrm{i}\theta_{1}}}{r_{2}e^{\mathrm{i}\theta_{2}}}=\frac{r_{1}}{r_{2}}e^{\mathrm{i}\left(\theta_{1}-\theta_{2}\right)}""", "r_1,r_2,theta_1,theta_2", "Divide magnitudes and subtract arguments."))
    add(expandedFormula(complex, "Roots", "Sum of roots of unity", """\sum_{k=0}^{n-1}e^{\frac{2\pi\mathrm{i}k}{n}}=0""", "k,n", "Use symmetry of all nth roots of unity.", KnowledgeLevel.UG))
    add(expandedFormula(complex, "Complex Functions", "Complex exponential", """e^{x+\mathrm{i}y}=e^{x}\left(\cos y+\mathrm{i}\sin y\right)""", "x,y", "Evaluate an exponential with a complex argument.", KnowledgeLevel.UG))
    add(expandedFormula(complex, "Complex Functions", "Complex sine", """\sin z=\frac{e^{\mathrm{i}z}-e^{-\mathrm{i}z}}{2\mathrm{i}}""", "z", "Define sine for a complex argument.", KnowledgeLevel.PG))
    add(expandedFormula(complex, "Complex Functions", "Complex cosine", """\cos z=\frac{e^{\mathrm{i}z}+e^{-\mathrm{i}z}}{2}""", "z", "Define cosine for a complex argument.", KnowledgeLevel.PG))
    add(expandedFormula(complex, "Residues and Contours", "Cauchy integral formula", """f\left(a\right)=\frac{1}{2\pi\mathrm{i}}\oint_{C}\frac{f\left(z\right)}{z-a}\,dz""", "f,a,C,z", "Evaluate an analytic function from contour values.", KnowledgeLevel.PG))
    add(expandedFormula(complex, "Residues and Contours", "Residue theorem", """\oint_{C}f\left(z\right)\,dz=2\pi\mathrm{i}\sum_{k}\operatorname{Res}\left(f,a_{k}\right)""", "C,f,z,k,a", "Evaluate contour integrals using enclosed singularities.", KnowledgeLevel.PG))
    add(expandedFormula(complex, "Residues and Contours", "Simple pole residue", """\operatorname{Res}\left(f,a\right)=\lim_{z\to a}\left(z-a\right)f\left(z\right)""", "f,a,z", "Find the residue at a simple pole.", KnowledgeLevel.PG))
    add(expandedFormula(complex, "Complex Analysis Basics", "Complex derivative", """f'\left(z\right)=\lim_{h\to0}\frac{f\left(z+h\right)-f\left(z\right)}{h}""", "f,z,h", "Define differentiability in the complex plane.", KnowledgeLevel.PG))

    val numerical = FormulaCategory.NumericalMethods
    add(expandedFormula(numerical, "Finite Differences", "Backward difference", """f'\left(x\right)\approx\frac{f\left(x\right)-f\left(x-h\right)}{h}""", "f,x,h", "Approximate a derivative from current and previous samples.", KnowledgeLevel.UG))
    add(expandedFormula(numerical, "Finite Differences", "Second central difference", """f''\left(x\right)\approx\frac{f\left(x+h\right)-2f\left(x\right)+f\left(x-h\right)}{h^{2}}""", "f,x,h", "Approximate a second derivative.", KnowledgeLevel.UG))
    add(expandedFormula(numerical, "Error Analysis", "Richardson extrapolation", """R=\frac{2^{p}A\left(\frac{h}{2}\right)-A\left(h\right)}{2^{p}-1}""", "R,p,A,h", "Cancel a leading discretization error term.", KnowledgeLevel.PG))
    add(expandedFormula(numerical, "Linear Systems", "Jacobi iteration", """x_{i}^{\left(k+1\right)}=\frac{1}{a_{ii}}\left(b_{i}-\sum_{j\ne i}a_{ij}x_{j}^{\left(k\right)}\right)""", "x,i,k,a,b,j", "Iteratively solve a diagonally dominant linear system.", KnowledgeLevel.UG))
    add(expandedFormula(numerical, "Linear Systems", "Gauss Seidel iteration", """x_{i}^{\left(k+1\right)}=\frac{1}{a_{ii}}\left(b_{i}-\sum_{j<i}a_{ij}x_{j}^{\left(k+1\right)}-\sum_{j>i}a_{ij}x_{j}^{\left(k\right)}\right)""", "x,i,k,a,b,j", "Solve a linear system using updated values immediately.", KnowledgeLevel.UG))
    add(expandedFormula(numerical, "Numerical Integration", "Gaussian quadrature", """\int_{-1}^{1}f\left(x\right)\,dx\approx\sum_{i=1}^{n}w_{i}f\left(x_{i}\right)""", "f,x,w,i,n", "Approximate an integral with optimal nodes and weights.", KnowledgeLevel.PG))
    add(expandedFormula(numerical, "Numerical Integration", "Monte Carlo integration", """I\approx\frac{V}{N}\sum_{i=1}^{N}f\left(\mathbf{x}_{i}\right)""", "I,V,N,i,f,x", "Estimate a high-dimensional integral by random sampling.", KnowledgeLevel.PG))
    add(expandedFormula(numerical, "ODE Solvers", "Improved Euler method", """y_{n+1}=y_{n}+\frac{h}{2}\left[f\left(x_{n},y_{n}\right)+f\left(x_{n+1},y_{n}+hf\left(x_{n},y_{n}\right)\right)\right]""", "y,n,h,f,x", "Approximate an ODE with a second-order predictor-corrector.", KnowledgeLevel.UG))
    add(expandedFormula(numerical, "Optimization", "Newton optimization", """\mathbf{x}_{k+1}=\mathbf{x}_{k}-H_{f}\left(\mathbf{x}_{k}\right)^{-1}\nabla f\left(\mathbf{x}_{k}\right)""", "x,k,H,f", "Find a local optimum using gradient and curvature.", KnowledgeLevel.PG))
    add(expandedFormula(numerical, "Optimization", "Golden section ratio", """\varphi=\frac{1+\sqrt{5}}{2}""", "phi", "Reduce a unimodal search interval efficiently."))
    add(expandedFormula(numerical, "Interpolation", "Cubic spline segment", """S_{i}\left(x\right)=a_{i}+b_{i}\left(x-x_{i}\right)+c_{i}\left(x-x_{i}\right)^{2}+d_{i}\left(x-x_{i}\right)^{3}""", "S,i,x,a,b,c,d", "Interpolate smoothly between adjacent data points.", KnowledgeLevel.PG))
    add(expandedFormula(numerical, "Root Finding", "Newton system update", """\mathbf{x}_{k+1}=\mathbf{x}_{k}-J_{F}\left(\mathbf{x}_{k}\right)^{-1}\mathbf{F}\left(\mathbf{x}_{k}\right)""", "x,k,J,F", "Solve a nonlinear system with a Jacobian.", KnowledgeLevel.PG))
}
