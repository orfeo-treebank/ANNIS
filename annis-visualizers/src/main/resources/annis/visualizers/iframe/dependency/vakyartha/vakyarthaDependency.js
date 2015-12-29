/*!
 * vakyartha dependency script 1.1
 * http://arborator.ilpga.fr/
 *
 * Copyright 2010, Kim Gerdes
 *
 * This program is free software:
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

///////////////////////// parameters ////////////////
////////////////////////////////////////////////////
tab=12; 			// space between tokens
line=25 		// line height
dependencyspace=180; 	// y-space for dependency representation
xoff=8; 		// placement of the start of a depdency link in relation to the center of the word
linedeps=15; 		// y distance between two arrows arriving at the same token (rare case)
pois=4; 		// size of arrow pointer
tokdepdist=15; 		// distance between tokens and depdendency relation
funccurvedist=8;	// distance between the function name and the curves highest point
depminh = 20; 		// minimum height for dependency
worddistancefactor = 8; // distant words get higher curves. this factor fixes how much higher.
defaultattris={
  "font": '14px "Arial"',
  "text-anchor":'start'
};
attris = {
  "t":		{
    "font": '18px "Arial"',
    "text-anchor":'start'
  },
  "cat":	{
    "font": '14px "Times"',
    "text-anchor":'start',
    "fill": '#036'
  },
  "lemma":	{
    "font": '14px "Times"',
    "text-anchor":'start',
    "fill": '#036'
  },
  "depline":	{
    "stroke": '#999',
    "stroke-width":'1'
  },
  "deptext":	{
    "font": '12px "Times"',
    "font-style":'italic',
    "fill": '#999'
  },
  "form":	{
    "font-style":'italic'
  }
};

///////////////////////// node object and functions ////////////////
////////////////////////////////////////////////////////////////////
function Pnode(c,b,visible){
  this.index=c;
  this.govs={};

  if("govs"in b)this.govs=b["govs"];
  this.width=0;
  if (visible) {
    this.texts=new Object();
    this.svgs=new Object();
    var d=dependencyspace;
    for(var h in shownfeatures){
      var e=shownfeatures[h];
      this.texts[e]= b[e];
      var f=paper.text(currentx,d,b[e]);
      f.attr(defaultattris);
      f.attr("title", b.tooltip);
      if(e in b)f.attr(attris[e]);
      if("attris"in b)f.attr(b["attris"][e]);
      var g=f.getBBox().width;
      f.width=g;
      f.index=c;
      this.svgs[e]=f;
      if(g>this.width)this.width=g;
      d=d+line
    };

    svgwi=svgwi+this.width+tab*2;
    this.x=0;
    this.y=0;
    this.svgdep={};
    f.deptextattris={};
  }

  this.deplineattris={};

  if("attris"in b&&"depline"in b["attris"])this.deplineattris=b["attris"]["depline"];
  if("attris"in b&&"deptext"in b["attris"])this.deptextattris=b["attris"]["deptext"]
};

drawsvgDep=function(c,b,d,h,e,f,g,i,p){
  var l=paper.set();
  var q=Math.abs(d-e)/2;
  var m=Math.max(h-q-worddistancefactor*Math.abs(c-b),-tokdepdist);
  m=Math.min(m,h-depminh);
  var r="M"+d+","+h+"C"+d+","+m+" "+e+","+m+" "+e+","+f;
  var j=paper.path(r).attr(attris["depline"]).attr({
    "x":d,
    "y":h
  });
  var n=paper.pointer(e,f,pois,0).attr(attris["depline"]);
  a=j.getPointAtLength(j.getTotalLength()/2);
  t=paper.text(a.x,a.y-funccurvedist,g);
  t.attr(attris["deptext"]);
  t.attr(p);
  t.index=c;
  t.govind=b;
  if(c==b){
    var k=a.x+t.getBBox().width/2+funccurvedist/2;
    if(k+t.getBBox().width/2>svgwi)k=a.x-t.getBBox().width/2-funccurvedist/2;
    if(k-t.getBBox().width/2<0)k=a.x;
    t.attr("x",k)
  }
  if(g in fcolors){
    var o="#"+fcolors[g];
    j.attr({
      stroke:o
    });
    n.attr({
      stroke:o
    });
    t.attr({
      fill:o
    })
  };

  j.attr(i);
  n.attr(i);
  l.push(t);
  l.push(j);
  l.push(n);
  return l
};

drawDep=function(c,b,d,h){
  c=parseInt(c);
  var offset;
  if(b=="root"){
    offset=0;
  }else if (b=="left"){
    offset=-20;
  }else if (b=="right"){
    offset=20;
  }
  if (offset===undefined){
    b=parseInt(b);
    var node1=words[b];
    var node2=words[c];
    if(node1==null){
      console.log("Delete #"+b+" from govs of words["+c+"]");
      delete node2.govs[b];
      return
    }
    if(c<b)var g=node1.svgs[shownfeatures[0]].attr("x")+node1.svgs[shownfeatures[0]].width/2-xoff;else var g=node1.svgs[shownfeatures[0]].attr("x")+node1.svgs[shownfeatures[0]].width/2+xoff;
    var e=node2.svgs[shownfeatures[0]].attr("x")+node2.svgs[shownfeatures[0]].width/2;
    var i=node1.svgs[shownfeatures[0]].attr("y")+pois-tokdepdist;
    var f=node2.svgs[shownfeatures[0]].attr("y")-tokdepdist-h*linedeps
  }else{
    b=c;
    node2=words[c];
    var e=node2.svgs[shownfeatures[0]].attr("x")+node2.svgs[shownfeatures[0]].width/2;
    var f=node2.svgs[shownfeatures[0]].attr("y")-tokdepdist;
    var g=e+offset;
    var i=0
  };

  node2.svgdep[b]=drawsvgDep(c,b,g,i,e,f,d,node2.deplineattris,node2.deptextattris)
};


drawsvgOutgoing=function(c,d,h,e,f,g,i,p,rot,num){
  var l=paper.set();
  var q=Math.abs(d-e)/2;
  var m=Math.max(h-q,-tokdepdist);
  if(rot>0){
    h+=num*20;
    m=20+num*20;
    var r="M"+d+","+h+"C"+d+","+m+" "+e+","+m+" "+e+","+f;
    var j=paper.path(r).attr(attris["depline"]).attr({
      "x":d,
      "y":h
    });
    var n=paper.pointer(d,h,pois,rot).attr(attris["depline"]);
  } else {
    m=Math.min(m,h-depminh);
    var r="M"+d+","+h+"C"+d+","+m+" "+e+","+m+" "+e+","+f;
    var j=paper.path(r).attr(attris["depline"]).attr({
      "x":d,
      "y":h
    });
    var n=paper.pointer(e,f,pois,0).attr(attris["depline"]);
  }
  a=j.getPointAtLength(j.getTotalLength()/2);
  // magic 20 point correction for right-pointing arrows only
  t=paper.text(a.x,a.y-funccurvedist+(d>e ? 20 : 0),g);
  t.attr(attris["deptext"]);
  t.attr(p);
  t.index=c;
  t.govind=c;

  var k=a.x+t.getBBox().width/2+funccurvedist/2;
  if(k+t.getBBox().width/2>svgwi)k=a.x-t.getBBox().width/2-funccurvedist/2;
  if(k-t.getBBox().width/2<0)k=a.x;
  t.attr("x",k)

  if(g in fcolors){
    var o="#"+fcolors[g];
    j.attr({
      stroke:o
    });
    n.attr({
      stroke:o
    });
    t.attr({
      fill:o
    })
  };

  j.attr(i);
  n.attr(i);
  l.push(t);
  l.push(j);
  l.push(n);
  return l
};

drawOutgoing=function(c,b,d,h,num){
  c=parseInt(c);
  node2=words[c];
  var e=node2.svgs[shownfeatures[0]].attr("x")+node2.svgs[shownfeatures[0]].width/2;

  var f=node2.svgs[shownfeatures[0]].attr("y")-tokdepdist+pois;
  var gap=Math.min(20,node2.svgs[shownfeatures[0]].width/2+1);
  var r=(gap<20?(gap<10 ? 150 : 130):110);
  if (b=="left"){
    var g=e-gap;
    var i=20;
    var rot=r;
  }else if (b=="right"){
    var g=e+gap;
    var i=20;
    var rot=360-r;
  }

  node2.svgdep[b]=drawsvgOutgoing(c,g,i,e,f,d,node2.deplineattris,node2.deptextattris,rot,num)
};



drawalldeps=function(){
  for(var c in words){
    var b=words[c];
    var d=0;
    var isRoot=true;
    for(var c in b.govs){
      isRoot = false;
      // check if this node and its governor are inside the visible context
      if (isVisible(c)) {
        if (isVisible(b.index)) {
          // both visible, always draw
          drawDep(b.index,c,b.govs[c],d);
        } else if (showoutgoing) {
          // outgoing arrow: governor is inside
          var pp = b.index<minvisible ? "left" : "right";
          if (!(pp in words[c])) {
            words[c][pp]=[];
          }
          words[c][pp].push({"idx": b.index, "text" : b.govs[c], "d": d});
        }
      } else if (isVisible(b.index) && showincoming) {
          // incoming arrow: governor is outside
          drawDep(b.index,c<minvisible ? "left" : "right",b.govs[c],d);
      }
      d+=1
    }
    if (isRoot && isVisible(b.index) && showroot) {
       drawDep(b.index,"root","root",d);
    }
  }
  // Draw deferred outgoing links.
  if (showoutgoing) {
    for (var c in words) {
      if (isVisible(c)) {
        if ("left" in words[c]) {
          var d = words[c]["left"];
          var dep;
          var count=0;
          while (dep = d.shift()) {
            drawOutgoing(c,"left",dep["text"],dep["d"],count);
            count++;
          }
        }
        if ("right" in words[c]) {
          var d = words[c]["right"];
          var dep;
          var count=0;
          while (dep = d.pop()) {
            console.log("  Draw dependency to outside node %o (RIGHT) from visible node %o, text '%o'.",
              dep["idx"],c,dep["text"]);
            drawOutgoing(c,"right",dep["text"],dep["d"],count);
            count++;
          }
        }
      }
    }
  }
};

words=new Object();
makewords=function(){
  svgwi=0;
  currentx=tab;
  for(var c in tokens){
      var b=new Pnode(c,tokens[c],isVisible(c));
      words[c]=b;
      if (isVisible(c)) {
        currentx=currentx+b.width+tab
      }
  }
}

function isVisible(n) {
  if (this['minvisible']===undefined || this['maxvisible'===undefined]) return true;
  return (n>=minvisible && n<=maxvisible);
}

Raphael.fn.pointer=function(c,b,d,h){
  var e=c+","+(b+d);
  var f="0,0"+(-d/2)+","+(-d*1.5)+" "+(-d/2)+","+(-d*1.5);
  var g=(d/2)+","+(d/2)+" "+(d/2)+","+(d/2)+" "+(d)+",0";
  var i=this.path("M"+e+"c"+f+"c"+g+"z");
  i.rotate(h);
  return i
}


/**
 * Calls the visualization, written by Kim Gerdes. Do not ask me, what is
 * happening here
 */
function drawDependenceTree()
{
  paper=Raphael("holder",window.innerWidth-100,100);
  svgpos=$("svg").offset();
  makewords();
  $("#holder").attr("style","background:white; position:relative;margin:0px; padding:0px");
  $("svg")[0].setAttribute("width",svgwi);
  $("svg")[0].setAttribute("height",dependencyspace+shownfeatures.length*line);
  drawalldeps();
}

$(function(){
  drawDependenceTree();
});
