/* 
 * Copyright 2011 Antidot opensource@antidot.net
 * https://github.com/antidot/db2triples
 * 
 * DB2Triples is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * DB2Triples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/***************************************************************************
 *
 * R2RML Model : Standard Referencing Object Map Class
 *
 * A referencing object map allows using the subjects
 * of another triples map as the objects generated by 
 * a predicate-object map.
 * 
 * modified by mielvandersande, andimou
 * 
 ****************************************************************************/
package be.ugent.mmlab.rml.model;

import java.util.HashSet;
import java.util.Set;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.exception.InvalidR2RMLStructureException;

public class StdReferencingObjectMap implements ReferencingObjectMap {

	private TriplesMap parentTriplesMap;
	private HashSet<JoinCondition> joinConditions;
	private PredicateObjectMap predicateObjectMap;
        protected TriplesMap ownTriplesMap;

	public StdReferencingObjectMap(PredicateObjectMap predicateObjectMap,
			TriplesMap parentTriplesMap, Set<JoinCondition> joinConditions) 
                throws InvalidR2RMLStructureException {
		setPredicateObjectMap(predicateObjectMap);
		this.parentTriplesMap = parentTriplesMap;
		setJoinConditions(joinConditions);
                setOwnTriplesMap(parentTriplesMap);

	}

	private void setJoinConditions(Set<JoinCondition> joinConditions) {
		this.joinConditions = new HashSet<JoinCondition>();
		this.joinConditions.addAll(joinConditions);
	}

        @Override
	public String getChildReference() {
		return predicateObjectMap.getOwnTriplesMap().getLogicalSource().getReference();
	}

        @Override
	public Set<JoinCondition> getJoinConditions() {
		return joinConditions;
	}

        @Override
	public String getJointReference() {
//		String jointSQLQuery = "SELECT * FROM (" + getChildQuery()
//				+ ") AS child, (" + getParentQuery() + ") AS parent";
//		// If the referencing object map has no join condition
//		if (joinConditions.isEmpty())
//			jointSQLQuery = "SELECT * FROM (" + getChildQuery() + ") AS tmp";
//		// If the referencing object map has at least one join condition
//		else {
//			String whereClause = " WHERE ";
//			int i = 0;
//			for (JoinCondition j : joinConditions) {
//				whereClause += "child." + j.getChild() + "=parent."
//						+ j.getParent();
//				i++;
//				if (i < joinConditions.size())
//					whereClause += " AND ";
//			}
//			jointSQLQuery += whereClause;
//		}
//		return jointSQLQuery;
            return "";
	}

        @Override
	public String getParentReference() {
		return parentTriplesMap.getLogicalSource().getReference();
	}

        @Override
	public TriplesMap getParentTriplesMap() {
		return parentTriplesMap;
	}

        @Override
	public PredicateObjectMap getPredicateObjectMap() {
		return predicateObjectMap;
	}

        @Override
	public void setPredicateObjectMap(PredicateObjectMap predicateObjectMap) {
		// Update predicateObjectMap if not contains this object map
		if (predicateObjectMap != null) {
			if (predicateObjectMap.getReferencingObjectMaps() == null)
				predicateObjectMap
						.setReferencingObjectMap(new HashSet<ReferencingObjectMap>());
			predicateObjectMap.getReferencingObjectMaps().add(this);
		}
		this.predicateObjectMap = predicateObjectMap;
	}

        @Override
        public void setOwnTriplesMap(TriplesMap ownTriplesMap)
			throws InvalidR2RMLStructureException {
		this.ownTriplesMap = ownTriplesMap;
	}

}
