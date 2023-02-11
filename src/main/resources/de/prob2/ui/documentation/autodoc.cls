\LoadClass[11pt]{article}
\usepackage{color}
\usepackage{listings}
\usepackage{tabularx}
\usepackage{longtable}
\usepackage[table]{xcolor}
\usepackage{colortbl}
\usepackage{tablestyles}
\usepackage{float}
\usepackage[style=1]{mdframed}
\usepackage[utf8]{inputenc}
\usepackage[T1]{fontenc}
\usepackage{changepage}
\usepackage{ragged2e, lipsum}
\usepackage{caption, booktabs}
\usepackage{graphicx}
\usepackage{hyperref}
\usepackage{hypgotoe}
\usepackage{amsfonts}
\usepackage{seqsplit}
%\catcode`\_=12 % underscores can be used without escaping them but  mathmode underscore where it serves to initiate a subscript term doesnt work anymore


\newcolumntype{L}[1]{>{\raggedright\let\newline\\\arraybackslash\hspace{0pt}}m{#1}}

\definecolor{MCHblue}{HTML}{0060A1}
\definecolor{MCHturquoise}{HTML}{00977E}
\definecolor{MCHpurple}{HTML}{970050}
\definecolor{MCHlightgreen}{HTML}{B4DE45}
\definecolor{MCHlila}{HTML}{8762FF}
\definecolor{editorGray}{rgb}{0.95, 0.95, 0.95}
\definecolor{editorOcher}{rgb}{1, 0.5, 0}
\definecolor{editorGreen}{rgb}{0, 0.5, 0}

\lstdefinelanguage{PROB}{
	keywords={PROPERTIES, MACHINE, SEES, INCLUDES, EXTENDS, IMPLEMENTS,PROMOTES,
			  REFINES,EVENTS,EVT,REF,EXTD,STATUS, DEFINITIONS,SETS, CONSTANTS, PROPERTIES,
			  AXIOMS, PROPERTIES, VALUES, VARIABLES,INVARIANT, INITIALISATION, OPERATIONS,TRUE,FALSE},
	keywordstyle=\color{MCHblue},
	keywords=[2]{SELECT,THEN,END,PRE,BEGIN,IF,ANY,WHERE,WHEN},
	keywordstyle=[2]\color{MCHpurple},
	keywords=[3]{or},
	keywordstyle=[3]\color{red},
	keywords=[4]{NATURAL,BOOL},
	keywordstyle=[4]\color{MCHlila},
	morecomment=[l]{//},
	morecomment=[s][\color{gray}]{/*}{*/ },
	sensitive=false,
	morestring=[b]",
	morestring=[b]',
	commentstyle=\color{gray}\ttfamily,
	literate=%
		%ADD UNICODE LITERATES %
		{∈}{{{\color{orange}$\in$}}}1
		{ℕ}{{{\color{orange}$\mathbb{N}$}}}1
		{⇒}{{{\color{orange}$\Rightarrow$}}}1
		{≠}{{{\color{orange}$\neq$}}}1
		{∀}{{{\color{orange}$\forall$}}}1
		{‥}{{{\color{orange}$\cdot\cdot$}}}1
		{∧}{{{\color{orange}$\land$}}}1
		%%
		{+}{{{\color{MCHturquoise}+}}}1
		{|}{{{\color{MCHlila}|}}}1
		{<}{{{\color{MCHturquoise}<}}}1
		{>}{{{\color{MCHturquoise}>}}}1
		{=}{{{\color{MCHturquoise}=}}}1
		{==}{{{\color{MCHturquoise}==}}}1
		{:}{{{\color{orange}:}}}1
		{\&}{{{\color{orange}\&}}}1
		{;}{{{\color{MCHblue};}}}1
		{:=}{{{\color{MCHblue}:=}}}1
		{/=}{{{\color{orange}/=}}}1
}

\lstdefinestyle{MCH}{
	language = PROB,
	basicstyle=\ttfamily\footnotesize,       % the size of the fonts that are used for the code
	numbers=left,                   % where to put the line-numbers
	numberstyle=\footnotesize,      % the size of the fonts that are used for the line-numbers
	stepnumber=1,                   % the step between two line-numbers. If it is 1 each line will be numbered
	numbersep=5pt,                  % how far the line-numbers are from the code
	backgroundcolor=\color{white},  % choose the background color. You must add \usepackage{color}
	showspaces=false,               % show spaces adding particular underscores
	showstringspaces=false,         % underline spaces within strings
	showtabs=false,                 % show tabs within strings adding particular underscores
	frame=single,           % adds a frame around the code
	stringstyle=\ttfamily\color{MCHlightgreen},
	tabsize=2,          % sets default tabsize to 2 spaces
	captionpos=b,           % sets the caption-position to bottom
	breaklines=true,        % sets automatic line breaking
	breakatwhitespace=true,    % sets if automatic breaks should only happen at whitespace
	escapeinside={\%*}{*)},          % if you want to add a comment within your code
	postbreak=\mbox{\textcolor{red}{$\hookrightarrow$}\space}
}

\lstdefinelanguage{JavaScript}{
	morekeywords={typeof, new, true, false, catch, function, return, null, catch, switch, var, if, in, while, do, else, case, break},
	morecomment=[s]{/*}{*/},
	morecomment=[l]//,
	morestring=[b]",
	morestring=[b]'
}
\lstdefinelanguage{HTML5}{
	language=html,
	sensitive=true,
	alsoletter={<>-},
	otherkeywords={
		% HTML tags
		<html>, <head>, <title>, </title>, <meta, />, </head>, <body>,<button>,<button,</button>,<div,<svg,<?xml,</metadata>
		<canvas, \/canvas>, <script>, </script>, </body>, </html>, <!, html>, <style>, </style>, ><,<progress,<div,<tr,
		<table, <th>,<tr>,</th>,</tr>,</div>,</table>,</button>
	},
	ndkeywords={=,
	% HTML attributes
	charset=, id=, width=, height=, xmlns:cc=,  xmlns:rdf=,  xmlns:svg=, xmlns:dc=,
	% CSS properties
	border:, font-family:,border-collapse:,text-align:,width:,padding:,background-color:,
	color:,outline:,content:,font-weight:,margin-left:,display:,overflow:,font-size:,float:,
	transform:, -moz-transform:, transition-duration:, transition-property:, transition-timing-function:
	},
	morecomment=[s]{<!--}{-->},
	tag=[s]
}

\lstdefinestyle{htmlAppendix}{%
	basicstyle=\footnotesize,       % the size of the fonts that are used for the code
	backgroundcolor=\color{white},  % choose the background color. You must add \usepackage{color}
	showspaces=false,               % show spaces adding particular underscores
	showstringspaces=false,         % underline spaces within strings
	showtabs=false,                 % show tabs within strings adding particular underscores
	frame=single,          % adds a frame around the code
% Code design
	keywordstyle=\color{blue}\bfseries,
	commentstyle=\color{darkgray}\ttfamily,
	ndkeywordstyle=\color{editorGreen}\bfseries,
	stringstyle=\color{editorOcher},
% Code
	language=HTML5,
	alsolanguage=JavaScript,
	alsodigit={.:;},
	tabsize=2,
	showtabs=false,
	showspaces=false,
	showstringspaces=false,
	extendedchars=true,
	breaklines=true,
	escapeinside={\%*}{*)},
	tabsize=2,          % sets default tabsize to 2 spaces
	breakatwhitespace=false,    % sets if automatic breaks should only happen at whitespace
}


\lstnewenvironment{tabularlstlisting}[1][]
{%
	\lstset{ %
		language=Java,                % choose the language of the code
		basicstyle=\scriptsize,       % the size of the fonts that are used for the code
		numbers=left,                   % where to put the line-numbers
		numberstyle=\footnotesize,      % the size of the fonts that are used for the line-numbers
		stepnumber=1,                   % the step between two line-numbers. If it is 1 each line will be numbered
		numbersep=5pt,                  % how far the line-numbers are from the code
		backgroundcolor=\color{white},  % choose the background color. You must add \usepackage{color}
		showspaces=false,               % show spaces adding particular underscores
		showstringspaces=false,         % underline spaces within strings
		showtabs=false,                 % show tabs within strings adding particular underscores
		frame=single,           % adds a frame around the code
		tabsize=2,          % sets default tabsize to 2 spaces
		captionpos=b,           % sets the caption-position to bottom
		breaklines=true,        % sets automatic line breaking
		breakatwhitespace=false,    % sets if automatic breaks should only happen at whitespace
		escapeinside={\%*}{*)}          % if you want to add a comment within your code
	}
	\lstset{aboveskip=-2ex,belowskip=-3ex,#1}%
}
{}


