/* 
 * Copyright 2019 Georgia Institute of Technology
 * All rights reserved.
 *
 * Author(s): Shaowei Zhu <swzhu@cc.gatech.edu>
 *
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package userpoweredflplugin;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;

public class SuspAnnotation extends Annotation {

	private static final String HIGH_SUSP = "userpoweredfl.ui.highSuspAnnotation";
	private static final String MID_SUSP = "userpoweredfl.ui.midSuspAnnotation";
	private static final String LOW_SUSP = "userpoweredfl.ui.lowSuspAnnotation";
//	private static final String NO_SUSP = "userpoweredfl.ui.noSuspAnnotation";

	private final Position position;
	private final ILine line;

	public SuspAnnotation(int offset, int length, ILine line) {
		super(getAnnotationID(line), false, null);
		this.line = line;
		position = new Position(offset, length);
	}

	public Position getPosition() {
		return position;
	}

	public ILine getLine() {
		return line;
	}

	public String getText() {
		return null;
	}

	private static String getAnnotationID(ILine line) {
		switch (line.getStatus()) {
		case -1:
		case 0:
			return null;
		case 1:
			return LOW_SUSP;
		case 2:
			return MID_SUSP;
		case 3:
			return HIGH_SUSP;
		}
		throw new AssertionError(line.getStatus());
	}

}
