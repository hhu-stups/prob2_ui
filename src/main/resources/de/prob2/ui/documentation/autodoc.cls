\LoadClass[11pt]{article}
\usepackage{listings}
\usepackage{tabularx}
\usepackage{longtable}
\usepackage[table]{xcolor}
\usepackage{float}
\usepackage{graphicx}
\usepackage{hyperref}
\usepackage{seqsplit}
\usepackage[utf8]{inputenc}
\usepackage{booktabs}
\usepackage{makecell}
\usepackage{caption}
\usepackage[T1]{fontenc}
\usepackage{mathptmx}
\usepackage[scaled=0.92]{helvet}
\usepackage{amsfonts}
\usepackage{amstext}
\hypersetup{pageanchor=false}
% Command can be used so underscores can be used without escaping them but
% so latexSafe Method can be deleted
% but mathmode underscore where it serves to initiate a subscript term doesnt work anymore
%\catcode`\_=12 %

\definecolor{headerGray}{RGB}{192,192,192}
\definecolor{rowGray}{RGB}{240,240,240}
\renewcommand\theadfont{\bfseries\sffamily} %% Sets styling of all table headers
\newcolumntype{P}[1]{>{\centering\arraybackslash}p{#1}} %%Enviroment to center table column with use of p
\newenvironment{tablestyling}{ %%Styling of tables can be changed here
	\rowcolors{1}{white}{rowGray}
	\renewcommand{\arraystretch}{1.3}
	\renewcommand\baselinestretch{1.1}\selectfont
	\small
	\sffamily
	\captionsetup{labelfont={bf}}
	}{}

%ENVIROMENT FOR CODE LISTINGS IN TABLE CELLS
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

%%STYLING OF MACHINE CODE LISTINGS
\definecolor{MCHblue}{HTML}{0060A1}
\definecolor{MCHturquoise}{HTML}{00977E}
\definecolor{MCHpurple}{HTML}{970050}
\definecolor{MCHlightgreen}{HTML}{B4DE45}
\definecolor{MCHlila}{HTML}{8762FF}

\lstdefinelanguage{PROB}{ %% Custom language for MCH Code
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
	commentstyle=\color{gray}\sffamily,
	literate=%
		%%% ADD UNICODE LITERATES HERE %%%
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

\lstdefinestyle{MCH}{ %Styling for MCH Code listing
	language = PROB,
	basicstyle=\sffamily\footnotesize,      % the size of the fonts that are used for the code
	numbers=left,                   % where to put the line-numbers
	numberstyle=\footnotesize,      % the size of the fonts that are used for the line-numbers
	stepnumber=1,                   % the step between two line-numbers. If it is 1 each line will be numbered
	numbersep=5pt,                  % how far the line-numbers are from the code
	backgroundcolor=\color{white},  % choose the background color. You must add \usepackage{color}
	showspaces=false,               % show spaces adding particular underscores
	showstringspaces=false,         % underline spaces within strings
	showtabs=false,                 % show tabs within strings adding particular underscores
	frame=single,           % adds a frame around the code
	stringstyle=\sffamily\color{MCHlightgreen},  % styling of strings
	rulecolor=\color{black},
	tabsize=2,          % sets default tabsize to 2 spaces
	captionpos=b,           % sets the caption-position to bottom
	breaklines=true,        % sets automatic line breaking
	breakatwhitespace=true,    % sets if automatic breaks should only happen at whitespace
	escapeinside={\%*}{*)},          % if you want to add a comment within your code
	postbreak=\mbox{\textcolor{red}{$\hookrightarrow$}\space}
}

%%% LISTING STYLES FOR HTML APPENDIX %%%
\definecolor{editorOcher}{rgb}{1, 0.5, 0}
\definecolor{editorGreen}{rgb}{0, 0.5, 0}

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
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%




