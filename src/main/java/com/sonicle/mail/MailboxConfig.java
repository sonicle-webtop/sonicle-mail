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
package com.sonicle.mail;

import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class MailboxConfig {
	private final int poolSize;
	private final String userFoldersPrefix;
	private final String rootFolderName;
	private final String sentFolderName;
	private final String draftsFolderName;
	private final String trashFolderName;
	private final String spamFolderName;
	private final String archiveFolderName;
	
	public MailboxConfig(final Builder builder) {
		Check.notNull(builder, "builder");
		this.poolSize = builder.poolSize;
		this.userFoldersPrefix = builder.userFoldersPrefix;
		this.rootFolderName = builder.rootFolderName;
		this.sentFolderName = builder.sentFolderName;
		this.draftsFolderName = builder.draftsFolderName;
		this.trashFolderName = builder.trashFolderName;
		this.spamFolderName = builder.spamFolderName;
		this.archiveFolderName = builder.archiveFolderName;
	}
	
	public int getPoolSize() {
		return poolSize;
	}

	public String getUserFoldersPrefix() {
		return userFoldersPrefix;
	}

	public boolean hasExplicitFoldersPrefix() {
		return !StringUtils.isBlank(userFoldersPrefix);
	}

	public String getRootFolderName() {
		return rootFolderName;
	}

	public boolean hasAlternativeRootFolder() {
		return !StringUtils.isBlank(rootFolderName);
	}

	public String getSentFolderName() {
		return sentFolderName;
	}

	public String getDraftsFolderName() {
		return draftsFolderName;
	}

	public String getTrashFolderName() {
		return trashFolderName;
	}

	public String getSpamFolderName() {
		return spamFolderName;
	}

	public String getArchiveFolderName() {
		return archiveFolderName;
	}
	
	public static class Builder {
		private int poolSize = 1;
		private String userFoldersPrefix = null;
		private String rootFolderName = null;
		private String sentFolderName = null;
		private String draftsFolderName = null;
		private String trashFolderName = null;
		private String spamFolderName = null;
		private String archiveFolderName = null;
		
		public MailboxConfig build() {
			return new MailboxConfig(this);
		}

		public Builder withPoolSize(int poolSize) {
			this.poolSize = poolSize;
			return this;
		}

		public Builder withUserFoldersPrefix(String userFoldersPrefix) {
			this.userFoldersPrefix = userFoldersPrefix;
			return this;
		}

		public Builder withRootFolderName(String rootFolderName) {
			this.rootFolderName = rootFolderName;
			return this;
		}

		public Builder withSentFolderName(String sentFolderName) {
			this.sentFolderName = sentFolderName;
			return this;
		}
		
		public Builder withDraftsFolderName(String draftsFolderName) {
			this.draftsFolderName = draftsFolderName;
			return this;
		}

		public Builder withTrashFolderName(String trashFolderName) {
			this.trashFolderName = trashFolderName;
			return this;
		}

		public Builder withSpamFolderName(String spamFolderName) {
			this.spamFolderName = spamFolderName;
			return this;
		}

		public Builder withArchiveFolderName(String archiveFolderName) {
			this.archiveFolderName = archiveFolderName;
			return this;
		}
	}
}
