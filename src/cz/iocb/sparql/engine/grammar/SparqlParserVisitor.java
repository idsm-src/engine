// Generated from SparqlParser.g4 by ANTLR 4.5.1
 package cz.iocb.sparql.engine.grammar; 
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SparqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SparqlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SparqlParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(SparqlParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#prologue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrologue(SparqlParser.PrologueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#baseDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseDecl(SparqlParser.BaseDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#prefixDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixDecl(SparqlParser.PrefixDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#selectQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectQuery(SparqlParser.SelectQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#subSelect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubSelect(SparqlParser.SubSelectContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(SparqlParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#selectVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectVariable(SparqlParser.SelectVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#constructQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructQuery(SparqlParser.ConstructQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#describeQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeQuery(SparqlParser.DescribeQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#describeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeClause(SparqlParser.DescribeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#askQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAskQuery(SparqlParser.AskQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#datasetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatasetClause(SparqlParser.DatasetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#whereClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhereClause(SparqlParser.WhereClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#solutionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSolutionModifier(SparqlParser.SolutionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#groupClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupClause(SparqlParser.GroupClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#groupCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupCondition(SparqlParser.GroupConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#havingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHavingClause(SparqlParser.HavingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#havingCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHavingCondition(SparqlParser.HavingConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#orderClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderClause(SparqlParser.OrderClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#orderCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderCondition(SparqlParser.OrderConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#limitOffsetClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimitOffsetClauses(SparqlParser.LimitOffsetClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#limitClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimitClause(SparqlParser.LimitClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#offsetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffsetClause(SparqlParser.OffsetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#valuesClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValuesClause(SparqlParser.ValuesClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#updateCommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdateCommand(SparqlParser.UpdateCommandContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#update}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate(SparqlParser.UpdateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#load}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoad(SparqlParser.LoadContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#clear}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClear(SparqlParser.ClearContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop(SparqlParser.DropContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#create}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate(SparqlParser.CreateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#add}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd(SparqlParser.AddContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#move}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMove(SparqlParser.MoveContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#copy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy(SparqlParser.CopyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#insertData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertData(SparqlParser.InsertDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#deleteData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteData(SparqlParser.DeleteDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#deleteWhere}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteWhere(SparqlParser.DeleteWhereContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#modify}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify(SparqlParser.ModifyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#deleteClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteClause(SparqlParser.DeleteClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#insertClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertClause(SparqlParser.InsertClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#usingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsingClause(SparqlParser.UsingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphOrDefault}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphOrDefault(SparqlParser.GraphOrDefaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphRef(SparqlParser.GraphRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphRefAll}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphRefAll(SparqlParser.GraphRefAllContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#quadPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuadPattern(SparqlParser.QuadPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#quadData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuadData(SparqlParser.QuadDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#quads}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuads(SparqlParser.QuadsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#quadsDetails}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuadsDetails(SparqlParser.QuadsDetailsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#quadsNotTriples}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuadsNotTriples(SparqlParser.QuadsNotTriplesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#triplesTemplate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriplesTemplate(SparqlParser.TriplesTemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#groupGraphPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#groupGraphPatternSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupGraphPatternSub(SparqlParser.GroupGraphPatternSubContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#groupGraphPatternSubList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupGraphPatternSubList(SparqlParser.GroupGraphPatternSubListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#triplesBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriplesBlock(SparqlParser.TriplesBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphPatternNotTriples}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphPatternNotTriples(SparqlParser.GraphPatternNotTriplesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#optionalGraphPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphGraphPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphGraphPattern(SparqlParser.GraphGraphPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#serviceGraphPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#bind}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBind(SparqlParser.BindContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#inlineData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineData(SparqlParser.InlineDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#dataBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataBlock(SparqlParser.DataBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#inlineDataOneVar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineDataOneVar(SparqlParser.InlineDataOneVarContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#inlineDataFull}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineDataFull(SparqlParser.InlineDataFullContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#dataBlockValues}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataBlockValues(SparqlParser.DataBlockValuesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#dataBlockValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataBlockValue(SparqlParser.DataBlockValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#minusGraphPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#groupOrUnionGraphPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter(SparqlParser.FilterContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint(SparqlParser.ConstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(SparqlParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#argList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgList(SparqlParser.ArgListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(SparqlParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#constructTemplate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructTemplate(SparqlParser.ConstructTemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#triplesSameSubject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#propertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyList(SparqlParser.PropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#propertyListNotEmpty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyListNotEmpty(SparqlParser.PropertyListNotEmptyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#verb}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVerb(SparqlParser.VerbContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#objectList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectList(SparqlParser.ObjectListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject(SparqlParser.ObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#triplesSameSubjectPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriplesSameSubjectPath(SparqlParser.TriplesSameSubjectPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#propertyListPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyListPath(SparqlParser.PropertyListPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#propertyListPathNotEmpty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyListPathNotEmpty(SparqlParser.PropertyListPathNotEmptyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#propertyListPathNotEmptyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyListPathNotEmptyList(SparqlParser.PropertyListPathNotEmptyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#verbPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVerbPath(SparqlParser.VerbPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#verbSimple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVerbSimple(SparqlParser.VerbSimpleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#objectListPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectListPath(SparqlParser.ObjectListPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#objectPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectPath(SparqlParser.ObjectPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPath(SparqlParser.PathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathAlternative}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathAlternative(SparqlParser.PathAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathSequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathSequence(SparqlParser.PathSequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathElt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathElt(SparqlParser.PathEltContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathEltOrInverse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathEltOrInverse(SparqlParser.PathEltOrInverseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathMod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathMod(SparqlParser.PathModContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathPrimary(SparqlParser.PathPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathNegatedPropertySet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathNegatedPropertySet(SparqlParser.PathNegatedPropertySetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#pathOneInPropertySet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathOneInPropertySet(SparqlParser.PathOneInPropertySetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#integer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInteger(SparqlParser.IntegerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#triplesNode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriplesNode(SparqlParser.TriplesNodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#blankNodePropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlankNodePropertyList(SparqlParser.BlankNodePropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#triplesNodePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriplesNodePath(SparqlParser.TriplesNodePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#blankNodePropertyListPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlankNodePropertyListPath(SparqlParser.BlankNodePropertyListPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection(SparqlParser.CollectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#collectionPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionPath(SparqlParser.CollectionPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphNode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphNode(SparqlParser.GraphNodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphNodePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphNodePath(SparqlParser.GraphNodePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#varOrTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarOrTerm(SparqlParser.VarOrTermContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#varOrIRI}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarOrIRI(SparqlParser.VarOrIRIContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar(SparqlParser.VarContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#graphTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraphTerm(SparqlParser.GraphTermContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#nil}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNil(SparqlParser.NilContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unarySignedLiteralExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnarySignedLiteralExpression(SparqlParser.UnarySignedLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code conditionalOrExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalOrExpression(SparqlParser.ConditionalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code additiveExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(SparqlParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unaryAdditiveExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryAdditiveExpression(SparqlParser.UnaryAdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code relationalExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(SparqlParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code relationalSetExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalSetExpression(SparqlParser.RelationalSetExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code baseExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseExpression(SparqlParser.BaseExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiplicativeExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(SparqlParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code conditionalAndExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalAndExpression(SparqlParser.ConditionalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unaryNegationExpression}
	 * labeled alternative in {@link SparqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryNegationExpression(SparqlParser.UnaryNegationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#unaryLiteralExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryLiteralExpression(SparqlParser.UnaryLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(SparqlParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(SparqlParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#builtInCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBuiltInCall(SparqlParser.BuiltInCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#regexExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegexExpression(SparqlParser.RegexExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#subStringExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubStringExpression(SparqlParser.SubStringExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#strReplaceExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrReplaceExpression(SparqlParser.StrReplaceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#existsFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExistsFunction(SparqlParser.ExistsFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#notExistsFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExistsFunction(SparqlParser.NotExistsFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#aggregate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate(SparqlParser.AggregateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#iriRefOrFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIriRefOrFunction(SparqlParser.IriRefOrFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#rdfLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRdfLiteral(SparqlParser.RdfLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#numericLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(SparqlParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#numericLiteralUnsigned}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteralUnsigned(SparqlParser.NumericLiteralUnsignedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#numericLiteralPositive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteralPositive(SparqlParser.NumericLiteralPositiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#numericLiteralNegative}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteralNegative(SparqlParser.NumericLiteralNegativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#booleanLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(SparqlParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(SparqlParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#iri}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIri(SparqlParser.IriContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#prefixedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixedName(SparqlParser.PrefixedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#blankNode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlankNode(SparqlParser.BlankNodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparqlParser#anon}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnon(SparqlParser.AnonContext ctx);
}