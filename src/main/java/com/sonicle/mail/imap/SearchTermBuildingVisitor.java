/*
 * sonicle-mail is is a helper library developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.mail.imap;

import com.sonicle.commons.flags.BitFlags;
import com.sonicle.commons.flags.BitFlagsEnum;
import com.sonicle.commons.rsql.parser.Operator;
import com.sonicle.commons.rsql.parser.ast.AndNode;
import com.sonicle.commons.rsql.parser.ast.ComparisonNode;
import com.sonicle.commons.rsql.parser.ast.LogicalNode;
import com.sonicle.commons.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import com.sonicle.commons.rsql.parser.ast.Node;
import com.sonicle.commons.rsql.parser.ast.OrNode;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.SearchTerm;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTimeZone;

/**
 *
 * @author malbinola
 */
public abstract class SearchTermBuildingVisitor extends NoArgRSQLVisitorAdapter<SearchTerm> {
	protected final Function<Object, Object> normalizer;
	protected BitFlags<DefaultSearchTermOption> defaultSearchTermOpts = BitFlags.noneOf(DefaultSearchTermOption.class);
	DateTimeZone timezone = DateTimeZone.getDefault();
	
	public SearchTermBuildingVisitor() {
		this(new DefaultNormalizer());
	}
	
	public SearchTermBuildingVisitor(Function<Object, Object> normalizer) {
		this.normalizer = normalizer;
	}
	
	public void setTimeZone(DateTimeZone dtz) {
		this.timezone = dtz;
	}
	
	abstract protected SearchTerm buildSearchTerm(final String fieldName, final Operator operator, final Collection<?> values, DateTimeZone timezone);
	
	@Override
	public SearchTerm visit(AndNode node) {
		SearchTerm[] children = node.getChildren().stream().map(this::visitAny).toArray(size -> new SearchTerm[size]);
		return new AndTerm(children);
	}

	@Override
	public SearchTerm visit(OrNode node) {
		SearchTerm[] children = node.getChildren().stream().map(this::visitAny).toArray(size -> new SearchTerm[size]);
		return new OrTerm(children);
	}

	@Override
	public SearchTerm visit(ComparisonNode node) {
		String fieldName = node.getSelector();
		Collection<?> values = node.getArguments().stream().map(normalizer).collect(Collectors.toList());
		SearchTerm ret = buildSearchTerm(fieldName, Operator.toOperator(node.getOperator()), values, timezone);
		//if (ret == null) throw new UnsupportedOperationException("Field not supported: " + fieldName);
		return ret;
	}
	
	private SearchTerm visitAny(Node node) {
		// skip straight to the children if it's a logical node with one member
		if (node instanceof LogicalNode) {
			LogicalNode ln = (LogicalNode)node;
			if (ln.getChildren().size() == 1) {
				return visitAny(ln.getChildren().get(0));
			}
		}
		
		if (node instanceof AndNode) {
			return visit((AndNode)node);
		} else if (node instanceof OrNode) {
			return visit((OrNode)node);
		} else {
			return visit((ComparisonNode)node);
		}
	}
	
	protected <T> SearchTerm toSearchTerm(final String term, final Operator operator, final Collection<?> values) {
		return toSearchTerm(term, operator, values, defaultSearchTermOpts);
	}
	
	protected <T> SearchTerm toSearchTerm(final String term, final Operator operator, final Collection<?> values, final BitFlags<DefaultSearchTermOption> opts) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	protected static class DefaultNormalizer implements Function<Object, Object> {

		@Override
		public Object apply(Object o) {
			return o;
		}
	}
	
	public static enum DefaultSearchTermOption implements BitFlagsEnum<DefaultSearchTermOption> {
		STRING_EQ_ICASE_COMP(1<<0), STRING_ANYOF_COMP(1<<2);
		
		private int mask = 0;
		private DefaultSearchTermOption(int mask) { this.mask = mask; }
		@Override
		public long mask() { return this.mask; }
		
		public static BitFlags<DefaultSearchTermOption> stringICaseComparison() {
			return BitFlags.with(DefaultSearchTermOption.STRING_EQ_ICASE_COMP);
		}
	}
}
