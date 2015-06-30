#-------------------------------------------------------------------
# Datavyu API v 1.07

# Please read the function headers for information on how to use them.

# CHANGE LOG
# 1.07 3/20/14 - Fixed a situation where argument names in mutex could overlap, causing a failure
#                 during argument rewrite
# 1.06 2/4/14 - Updated to work with new DB, added function for deleting cells
# 1.05 1/3/14 - Fixed the loadMacshapaDB function so it now works properly with CLOSED files
# 1.041 12/11/13 - Updated function names to be consistent with new terminology
#                  ('variable' functions renamed to 'column', 'arg' to code').
#                  Old names will still work.
# 1.04 11/25/13 - Fixed mutex function which was failing in some cases
# 1.03 08/27/13 - Fixed makeReliability and change_arg functions so they behave properly
# 1.02 07/07/13 - Fixed functions involving createVariable.
# 1.01 03/13/12 - Fixed the set variable function so it now correctly writes back to
#                 mongodb
# 1.0 07/24/12 - Updated API to work with new MongoDB. Also updated function names
#                such that they are more consistent. Old names should work, but are
#                now deprecated and may be removed in a later version.
# 0.995 02/28/12 - Fixed the print_debug statement and potentially fixed an issue
#                   with create_mutually_exclusive
# 0.994 01/24/12 - Added mutex method to identify and correct causes of inf loops.
# 0.993 11/28/11 - Fixed typo in Mutex, added in mutex error checking,
#                  and made all print statements available only when $debug=true
# 0.992 9/13/11 - CreateMutuallyExclusive now adds proper ordinals on
# 0.991 8/26/11 - Fixed an edge case where mutexing would miss a cell it should get.
#                 Also made the function jump times.  Should be MUCH faster.
# 0.99 7/6/11 - Totally rewrote create_mutually_exclusive function so it is faster
#                 and now works with point cells.  Also made some fixes in
#                 preparation for Datavyu 2.00.
# 0.984 2/16/11 - Fixed a heap error bug in mutex, several bugs with editing
#                 variable arguments.  Added functions for adding variable
#                 arguments, and framework for generic print script.  Several
#                 versions of incremental fixes.
# 0.98 10/10/10 - Added function to get list of columns, fixed up the import
#                 Macshapa function.  It should work for most files now.
# 0.97 8/11/10 -  Added a function to check for valid codes in a variable,
#                 and fixed a bug with check_rel.
# 0.96 8/11/10 -  Added a function to check reliability between two columns
#                 and print either to a file or to the console.
# 0.95 7/22/10 -  Added a function to transfer columns between files and
#                 added headers to functions that didn't have any.
# 0.94 7/22/10 -  Fixed the save_db function so it works with opf files
#                 and will detect if you are saving a csv file.
# 0.93 7/20/10 -  Merged in function to read MacSHAPA Closed database
#                 files into Datavyu.
# 0.92 6/29/10 -  Added function to delete columns
# 0.91 6/25/10 -  Added load functions, fixed some issues with Mutex
# =>              save still has some issues though; working out how to
# =>              access the project variables from Ruby.

# Licensing information:
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#-------------------------------------------------------------------

require 'java'
require 'csv'
require 'time'
require 'date'
require 'set'
#require 'ftools'

import 'org.datavyu.models.db.Datastore'
import 'org.datavyu.models.db.MatrixValue'
import 'org.datavyu.models.db.NominalValue'
import 'org.datavyu.models.db.TextValue'
import 'org.datavyu.models.db.Value'
import 'org.datavyu.models.db.Variable'
import 'org.datavyu.models.db.Cell'
import 'org.datavyu.models.db.Argument'
import 'org.datavyu.models.project.Project'
import 'org.datavyu.controllers.SaveC'
import 'org.datavyu.controllers.OpenC'
import 'org.datavyu.controllers.project.ProjectController'

$debug = false
def print_debug(*s)
	if $debug == true
		p s
	end
end

class RCell

	attr_accessor :ordinal, :onset, :offset, :arglist, :argvals, :db_cell, :parent


	#-------------------------------------------------------------------
	# Note: This method is not for general use, it is used only when creating
	#       this variable from the database in the getVariable method.
	#
	# Method name: set_args
	# Function: sets up methods that can be used to reference the arguments in
	#           the cell.
	# Arguments:
	# => argvals (required): Values of the arguments being created
	# => arglist (required): Names of the arguments being created
	#-------------------------------------------------------------------

	def set_args(argvals, arglist)
		@arglist = arglist
		@argvals = argvals
		i = 0
		if argvals == ""
			@argvals = Array.new
			arglist.each do |arg|
				@argvals << nil
			end
		end
		arglist.each do |arg|

			if @argvals[i].nil?
				@argvals[i] = ""
			end
			#Tricky magic part where we are defining var names on the fly.  Escaped quotes turn everything to strings.
			#Handle this later by allowing numbers to be numbers but keeping strings.

			instance_eval "def #{arg}; return argvals[#{i}]; end"
			i += 1
		end
	end

	def change_code_name(i, new_name)
		change_arg_name(i, new_name)
	end
	def change_arg_name(i, new_name)
		instance_eval "def #{new_name}; return argvals[#{i}]; end"
	end

	def add_code(new_name)
		add_arg(new_name)
	end
	def add_arg(new_name)
		@argvals << ""
		i = argvals.length - 1
		instance_eval "def #{new_name}; return argvals[#{i}]; end"
	end

	def remove_code(name)
		remove_arg(name)
	end
	def remove_arg(name)
		argvals.delete(arglist.index(name))
		@arglist.delete(name)
	end

	def get_code(name)
		get_arg(name)
	end
	def get_arg(name)
		return argvals[arglist.index(name)]
	end


	#-------------------------------------------------------------------
	# Method name: change_arg
	# Function: Changes the value of an argument in a cell.
	# Arguments:
	# => arg (required): Name of the argument to be changed
	# => val (required): Value to change the argument to
	# Returns:
	# => None
	# Usage:
	#       trial = getVariable("trial")
	#       trial.cells[0].change_arg("onset", 1000)
	#       setVariable("trial",trial)
	#-------------------------------------------------------------------
	def change_code(arg, val)
		change_arg(arg, val)
	end
	def change_arg(arg, val)
		if arg == "onset"
			@onset = val
		elsif arg == "offset"
			@offset = val
		elsif arg == "ordinal"
			@ordinal = val
		else
			for i in 0..arglist.length-1
				if arglist[i] == arg and not arg.nil?
					argvals[i] = val.to_s
				end
			end
		end
	end

	#-------------------------------------------------------------------
	# Method name: print_all
	# Function: Dumps all of the arguments in the cell to a string.
	# Arguments:
	# => p (optional): The seperator used between the arguments.  Defaults to tab (\t)
	# Returns:
	# => A string of the arguments starting with ordinal/onset/offset then argument.
	# Usage:
	#       trial = getVariable("trial")
	#       print trial.cells[0].print_all()
	#-------------------------------------------------------------------

	def print_all(*p)
		if p.empty?
			p << "\t"
		end
		print @ordinal.to_s + p[0] + @onset.to_s + p[0] + @offset.to_s + p[0]
		@arglist.each do |arg|
			t = eval "self.#{arg}"
			if t == nil
				v = ""
			else
				v = t
			end
			print v + p[0]
		end
	end

	#-------------------------------------------------------------------
	# Method name: is_within
	# Function: Is encased by outer_cell temporally
	# Arguments:
	# => outer_cell: check to see if this cell is in outer_cell
	# Returns:
	# => boolean
	# Usage:
	#       trial = getVariable("trial")
	#       id = getVariable("id")
	#       if trial.cells[0].is_within(id.cells[0])
	#           do something
	#       end
	#-------------------------------------------------------------------

	def is_within(outer_cell)
		if outer_cell.onset <= @onset and outer_cell.offset >= @offset
			return true
		else
			return false
		end
	end

	#-------------------------------------------------------------------
	# Method name: contains
	# Function: Check to see if this cell encases inner_cell temporally
	# Arguments:
	# => inner_cell: the cell to check if it is inside of this cell
	# Returns:
	# => boolean
	# Usage:
	#       trial = getVariable("trial")
	#       id = getVariable("id")
	#       if id.cells[0].contains(trial.cells[0])
	#           do something
	#       end
	#-------------------------------------------------------------------

	def contains(inner_cell)
		if inner_cell.onset >= @onset and inner_cell.offset <= @offset
			return true
		else
			return false
		end
	end

	#-------------------------------------------------------------------
	# Method name: duration
	# Function: Return the duration of this cell
	# Arguments: None
	# Usage:
	# 	duration = myCell.duration
	#-------------------------------------------------------------------
	def duration
		return @offset - @onset
	end
end

#-------------------------------------------------------------------
# Class name: Variable
# Function: This is the Ruby container for Datavyu variables.
#-------------------------------------------------------------------

class RVariable

	attr_accessor :name, :type, :cells, :arglist, :old_args, :dirty, :db_var

	#-------------------------------------------------------------------
	# NOTE: This function is not for general use.
	#
	# Method name: set_cells
	# Function: Creates the cell object in the Variable object.
	# Arguments:
	# => newcells (required): Array of cells coming from the database via getVariable
	# => arglist (required): Array of the names of the arguments from the database
	#-------------------------------------------------------------------
	def convert_argname(arg)
		return arg.gsub(/(\W)+/,"").downcase
	end

	def set_cells(newcells, arglist)
		print_debug "Setting cells"
		@cells = Array.new
		@arglist = Array.new
		arglist.each do |arg|
			# Regex to delete any character not a-z,0-9,or _
			print_debug arg
			if ["0","1","2","3","4","5","6","7","8","9"].include?(arg[0].chr)
				arg = "_" + arg
			end
			@arglist << arg.gsub(/(\W)+/,"").downcase
		end
		if !newcells.nil?
			ord = 0
			newcells.each do |cell|
				ord += 1
				c = RCell.new
				c.onset = cell.getOnset
				c.offset = cell.getOffset
				c.db_cell = cell
				c.parent = @name
				vals = Array.new
				if cell.getVariable.getRootNode.type == Argument::Type::MATRIX
					for val in cell.getValue().getArguments
						vals << val.toString
					end
				else
					vals << cell.getValue().toString
				end
				c.set_args(vals,@arglist)
				c.ordinal = ord
				@cells << c
			end
		end
	end

	#-------------------------------------------------------------------
	# Method name: make_new_cell
	# Function: Creates a new, blank cell at the end of this variable's cell array
	# Arguments:
	# => None
	# Returns:
	# => Reference to the cell that was just created.  Modify the cell using this reference.
	# Usage:
	#       trial = getVariable("trial")
	#       new_cell = trial.make_new_cell()
	#       new_cell.change_arg("onset", 1000)
	#       setVariable("trial", trial)
	#-------------------------------------------------------------------
	def make_new_cell()
		c = RCell.new
		c.onset = 0
		c.offset = 0
		c.ordinal = 0
		c.set_args("", @arglist)
		c.parent = @name
		@cells << c
		return c
	end

	def create_cell()
		make_new_cell()
	end

	def sort_cells()
		cells.sort! { |a,b| a.onset <=> b.onset }
	end


	#-------------------------------------------------------------------
	# Method name: change_arg_name
	# Function: Creates a new, blank cell at the end of this variable's cell array
	# Arguments:
	# => old_name: the name of the argument you want to change
	# => new_name: the name you want to change old_name to
	# Returns:
	# => nothing.
	# Usage:
	#       trial = getVariable("trial")
	#
	#-------------------------------------------------------------------
	def change_code_name(old_name, new_name)
		change_arg_name(old_name, new_name)
	end
	def change_arg_name(old_name, new_name)
		i = @old_args.index(old_name)
		@old_args[i] = new_name
		if ["0","1","2","3","4","5","6","7","8","9"].include?(old_name[1].chr)
			old_name = "_" + old_name
		end
		old_name = old_name.gsub(/(\W)+/,"").downcase

		i = @arglist.index(old_name)
		@arglist[i] = new_name
		for cell in @cells
			cell.change_arg_name(i, new_name)
		end

		@dirty = true
	end

	def add_code(name)
		add_arg(name)
	end
	def add_arg(name)
		@old_args << name
		if ["0","1","2","3","4","5","6","7","8","9"].include?(name[1].chr)
			name = "_" + name
		end
		name = name.gsub(/(\W)+/,"").downcase

		@arglist << name
		for cell in @cells
			cell.add_arg(name)
		end

		@dirty = true
	end

	def remove_code(name)
		remove_arg(name)
	end
	def remove_arg(name)
		@old_args.delete(name)

		name = name.gsub(/(\W)+/,"").downcase
		@arglist.delete(name)

		for cell in @cells
			cell.remove_arg(name)
		end

		@dirty = true
	end

end

#-------------------------------------------------------------------
# Method name: getVariable
# Function: getVariable retrieves a variable from the database and print_debug it into a Ruby object.
# Arguments:
# => name (required): The Datavyu name of the variable being retrieved
# Returns:
# => A Ruby object representation of the variable inside Datavyu or nil if the named column does not exist.
# Usage:
#       trial = getVariable("trial")
#-------------------------------------------------------------------

def getColumn(name)
	getVariable(name)
end
def getVariable(name)

	var = $db.getVariable(name)
	if (var == nil)
		printNoColumnFoundWarning(name.to_s)
		return nil
	end

	# Convert each cell into an array and store in an array of arrays
	cells = var.getCells()
	arg_names = Array.new

	# Now get the arguments for each of the cells

	# For matrix vars only
	type = var.getRootNode.type
	if type == Argument::Type::MATRIX
		# Matrix var
		arg_names = Array.new
		for arg in var.getRootNode.childArguments
			arg_names << arg.name
		end
	else
		# Nominal or text
		arg_names = ["var"]
	end

	v = RVariable.new
	v.name = name
	v.old_args = arg_names
	v.type = type
	v.set_cells(cells, arg_names)
	v.sort_cells
	v.dirty = false
	v.db_var = var

	return v
end

#-------------------------------------------------------------------
# Method name: setVariable
# Function: setVariable will overwrite a variable in the database with the same name as the name argument.
#           If no variable with the same name exists, it will create a new variable.
# Arguments:
# => name (optional): The name of the variable being created
# => var  (required): The Ruby container of the variable to be put into the database.  This is the return value of
#         createNewVariable or getVariable that has been modified.
# Usage:
#       trial = getVariable("trial")
#       ** Do some modification to trial
#       setVariable("trial", trial)
#-------------------------------------------------------------------
def setColumn(*args)
	setVariable(*args)
end
def setVariable(*args)

	if args.length == 1
		var = args[0]
		name = var.name
	elsif args.length == 2
		var = args[1]
		name = args[0]
	end

	# If substantial changes have been made to the structure of the column,
	# just delete the whole thing first.
	# If the column was dirty, redo the vocab too
	if var.db_var == nil or var.db_var.get_name != name

		if getColumnList().include?(name)
			deleteVariable(name)
		end
		# Create a new variable
		v = $db.createVariable(name, Argument::Type::MATRIX)
		var.db_var = v

		if var.arglist.length > 0
			var.db_var.removeArgument("code01")
		end

		# Set variable's vocab
		for arg in var.arglist
			new_arg = v.addArgument(Argument::Type::NOMINAL)
			new_arg.name = arg
			main_arg = var.db_var.getRootNode()
			child_args = main_arg.childArguments

			child_args.get(child_args.length-1).name = arg

			var.db_var.setRootNode(main_arg)
		end
		var.db_var = v
	end

	#p var
	if var.dirty
		# deleteVariable(name)
		# If the variable is dirty, then we have to do something to the vocab.
		# Compare the variable's vocab and the Ruby cell version to see
		# what is different.

		#p var.db_var
		if var.db_var.getRootNode.type == Argument::Type::MATRIX
			values = var.db_var.getRootNode.childArguments
			#p values
			for arg in var.old_args
				#p var.old_args
				flag = false
				for dbarg in values
					if arg == dbarg.name
						flag = true
						break
					end
				end
				# If we didn't find it in dbarg, we have to create it
				if flag == false
					# Add the argument
					new_arg = var.db_var.addArgument(Argument::Type::NOMINAL)

					# Make sure argument doesn't have < or > in it.
					arg = arg.delete("<").delete(">")
					# Change the argument's name by getting the variable back,
					# and then setting it. This hoop jumping is annoying.
					new_arg.name = arg
					main_arg = var.db_var.getRootNode()
					child_args = main_arg.childArguments

					child_args.get(child_args.length-1).name = arg

					var.db_var.setVariableType(main_arg)
				end
			end

			# Now see if we have deleted any arguments
			for dbarg in values
				flag = false
				for arg in var.old_args
					if arg == dbarg.name
						flag = true
						break
					end
				end

				# If we didn't find dbarg in old_args, then we must
				# have deleted it. Remove the argument from the DB
				if flag == false
					puts "DELETING ARG:" + dbarg.name
					var.db_var.removeArgument(dbarg.name)
				end
			end
		end


	end

	# Create new cells and fill them in for each cell in the variable
	for cell in var.cells
		# Copy the information from the ruby variable to the new cell

		if cell.db_cell == nil or cell.parent != name
			cell.db_cell = var.db_var.createCell()
		end

		value = cell.db_cell.getValue()

		if cell.onset != cell.db_cell.getOnset
			cell.db_cell.setOnset(cell.onset)
		end

		if cell.offset != cell.db_cell.getOffset
			cell.db_cell.setOffset(cell.offset)
		end

		# Matrix cell
		if cell.db_cell.getVariable.getRootNode.type == Argument::Type::MATRIX
			values = cell.db_cell.getValue().getArguments()
			for arg in var.old_args
				# Find the arg in the db's arglist that we are looking for
				for i in 0...values.size
					dbarg = values[i]
					dbarg_name = dbarg.getArgument.name
					if dbarg_name == arg and not ["", nil].include?(cell.get_arg(var.convert_argname(arg)))
						dbarg.set(cell.get_arg(var.convert_argname(arg)))
						break
					end
				end
			end

			# Non-matrix cell
		else
			value = cell.db_cell.getValue()
			value.set(cell.get_arg("var"))
		end

		# Save the changes back to the DB

	end


	#arg_names = var.old_args
	#cells = Array.new
	#var.cells.each do |cell|
	#c = Array.new
	#c << cell.onset
	#c << cell.offset
	#c << Array.new
	#var.arglist.each do |arg|
	#t = eval "cell.#{arg}"
	#c[2] << t.to_s()
	#end
	#cells << c
	#end
	#print_debug "creating column"
	## If the column already exists, delete it and build a new one.
	## If it doesn't, just add a new one.
	#if not $db.col_name_in_use(name)
	#col = DataColumn.new($db, name, MatrixVocabElement::MatrixType::MATRIX)
	#$db.add_column(col)
	#else
	#oldcol = $db.get_column(name)
	#numcells = oldcol.get_num_cells
	#numcells.downto(1) do |i|
	#$db.remove_cell($db.get_cell(oldcol.get_id, i).get_id)
	#end
	##$db.remove_column(oldcol.get_id)

	##col = DataColumn.new($db, name, MatrixVocabElement::MatrixType::MATRIX)
	##$db.add_column(col)
	#end
	## Check if matrix already defined
	#col = $db.get_column(name)
	#mve0 = $db.get_matrix_ve(col.its_mve_id)
	#if mve0.get_num_formal_args() == 1
	## Setup structure of matrix column
	#mve0 = MatrixVocabElement.new(mve0)

	#mve0.delete_formal_arg(0)
	#arg_names.each do |arg|
	#farg = NominalFormalArg.new($db, arg)
	#mve0.append_formal_arg(farg)
	#end

	#$db.replace_matrix_ve(mve0)
	#end
	#col = $db.get_column(name)
	#mve0 = $db.get_matrix_ve(col.its_mve_id)
	#matID0 = mve0.get_id()
	#cells.each do |cell|
	##print_debug "writing cell"
	#c = DataCell.new($db, col.get_id, matID0)
	#mat = Matrix.new($db, matID0)

	#if cell[0].to_i > 0
	#c.onset = TimeStamp.new(1000, cell[0].to_i)
	#end
	#if cell[1].to_i > 0
	#c.offset = TimeStamp.new(1000, cell[1].to_i)
	#end

	#narg = 0
	#cell[2].each do |dv|
	#argid = mve0.get_formal_arg(narg).get_id()
	#if dv == "" or dv == nil
	#a = arg_names[narg]
	#fdv = NominalDataValue.new($db, argid)
	#fdv.clearValue()
	#else
	#fdv = NominalDataValue.new($db, argid, dv)
	#end

	#mat.replaceArg(narg,fdv)
	#narg += 1
	#end
	#c.set_val(mat)
	#$db.append_cell(c)
	#end
end

#-------------------------------------------------------------------
# Method name: make_rel
# Function: This function will create a reliability column that is a copy
#           of another column in the database, copying every nth cell and
#           carrying over some of the arguments from the original, if wanted.
# Arguments:
# => relname (required): The name of the reliability column to be created.
# => var_to_copy (required): The name of the variable in the database you
#                   wish to copy.
# => multiple_to_keep: The number of cells to skip.  For every other cell, use 2.
# => *args_to_keep: Comma separated strings for the arguments you want to keep
#             between cells.  For example, "onset", "trialnum", "block" would keep
#             those three arguments in the new cells that are created.
# Returns:
# => A Ruby object representation of the rel column inside Datavyu.
# Usage:
#       rel_trial = make_rel("rel.trial", "trial", 2, "onset", "trialnum", "unit")
#-------------------------------------------------------------------
def make_rel(relname, var_to_copy, multiple_to_keep, *args_to_keep)
	makeReliability(relname, var_to_copy, multiple_to_keep, args_to_keep)
end
def makeReliability(relname, var_to_copy, multiple_to_keep, *args_to_keep)
	# Get the primary variable from the DB

	if var_to_copy.class == String
		var_to_copy = getVariable(var_to_copy)
	else
		var_to_copy = getVariable(var_to_copy.name)
	end

	if args_to_keep[0].class == Array
		args_to_keep = args_to_keep[0]
	end

	# Clip down cells to fit multiple to keep
	for i in 0..var_to_copy.cells.length-1
		if multiple_to_keep == 0
			var_to_copy.cells[i] = nil
		elsif var_to_copy.cells[i].ordinal % multiple_to_keep != 0
			var_to_copy.cells[i] = nil
		else
			var_to_copy.cells[i].ordinal = var_to_copy.cells[i].ordinal / multiple_to_keep
		end
	end
	# Clear out the nil cells
	var_to_copy.cells.compact!

	var_to_copy.cells.each do |cell|
		if !args_to_keep.include?("onset")
			cell.onset = 0
		end
		if !args_to_keep.include?("offset")
			cell.offset = 0
		end
		cell.arglist.each do |arg|
			if !args_to_keep.include?(arg)
				cell.change_arg(arg,"")
			end
		end
	end
	setVariable(relname, var_to_copy)
	return var_to_copy
end

#-------------------------------------------------------------------
# Method name: createNewVariable
# Function: Creates a brand new blank variable with argument *args and name name.
# Arguments:
# => name (required): The Datavyu name of the variable being retrieved
# => *args: (optional): List of arguments that the variable will contain.  Onset, Offset, and
#               ordinal are created by default.
# Returns:
# => A Ruby object representation of the variable inside Datavyu.
# Usage:
#       trial = createNewVariable("trial", "trialnum", "unit")
#       blank_cell = trial.make_new_cell()
#       setVariable(trial)
#-------------------------------------------------------------------
def createNewColumn(name, *args)
	createVariable(name, *args)
end
def createColumn(name, *args)
	createVariable(name, *args)
end

def createNewVariable(name, *args)
	createVariable(name, args)
end

def createVariable(name, *args)
	print_debug "Creating new variable"
	v = RVariable.new

	v.name = name

	v.dirty = true

	print_debug args[0].class
	print_debug args
	if args[0].class == Array
		args = args[0]
	end
	print_debug args

	# Set the argument names in arg_names and set the database internal style with <argname> in old_args
	arg_names = Array.new
	old_args = Array.new
	for arg in args
		print_debug arg
		arg_names << arg
		old_args << arg.to_s
	end
	c = Array.new
	v.old_args = old_args
	v.set_cells(nil, arg_names)

	# Return reference to this variable for the user
	print_debug "Finished creating variable"
	return v
end

#-----------------------------------------------------------------
# EXPERIMENTAL METHODS FOR FUTURE RELEASE
#-----------------------------------------------------------------

#-----------------------------------------------------------#
# make_duration_rel: Makes a duration based reliability column
# based on John's method.  It will create two new columns, one
# that contains a cell with a number for that block, and another
# blank column for the free coding within that block.
#-----------------------------------------------------------#

#-------------------------------------------------------------------
# Method name: makeDurationBlockRel
# Function: Makes a duration based reliability column
# based on John's method.  It will create two new columns, one
# that contains a cell with a number for that block, and another
# blank column for the free coding within that block.
# Arguments:
# => relname (required): The name of the rel column to be made.
# => var_to_copy (required): The name of the variable being copied.
# => binding (required): The name of the variable to bind the copy to.
# => block_dur (required): How long (in seconds) should the blocks be?
# => skip_blocks (required): How many blocks of block_dur should we skip between
#     each coding block?
#
# # Returns:
# => Nothing.  Variables are written to the database.
# #-------------------------------------------------------------------
def makeDurationBlockRel(relname, var_to_copy, binding, block_dur, skip_blocks)
	block_var = createVariable(relname + "_blocks", "block_num")
	rel_var = make_rel(relname, var_to_copy, 0)

	var_to_copy = getVariable(var_to_copy)
	binding = getVariable(binding)


	block_dur = block_dur * 1000 # Convert to milliseconds
	block_num = 1
	for bindcell in binding.cells
		cell_dur = bindcell.offset - bindcell.onset
		if cell_dur <= block_dur
			cell = block_var.make_new_cell()
			cell.change_arg("block_num", block_num.to_s)
			cell.change_arg("onset", bindcell.onset)
			cell.change_arg("offset", bindcell.offset)
			block_num += 1
		else
			num_possible_blocks = cell_dur / block_dur  #Integer division
			if num_possible_blocks > 0
				for i in 0..num_possible_blocks
					if i % skip_blocks == 0
						cell = block_var.make_new_cell()
						cell.change_arg("block_num", block_num.to_s)
						cell.change_arg("onset", bindcell.onset + i * block_dur)
						if bindcell.onset + (i + 1) * block_dur <= bindcell.offset
							cell.change_arg("offset", bindcell.onset + (i + 1) * block_dur)
						else
							cell.change_arg("offset", bindcell.offset)
						end
						block_num += 1
					end
				end
			end
		end
	end
	setVariable(relname + "_blocks", block_var)
end

#-------------------------------------------------------------------
# Method name: add_args_to_var
# Function: Add new arguments to any variable
# Arguments:
# => var (required): The variable to add args to.  This can be a name or a variable object.
# => *args (required): A list of the arguments to add to var (can be any number of args)
#
# Returns:
# => The new Ruby representation of the variable.  Write it back to the database
# to save it.
#
# Example:
# test = add_args_to_var("test", "arg1", "arg2", "arg3")
# setVariable("test",test)
# -------------------------------------------------------------------
def add_codes_to_column(var, *args)
	add_args_to_var(var, *args)
end

def add_args_to_var(var, *args)
	if var.class == "".class
		var = getVariable(var)
	end

	var_new = createVariable(var.name, var.arglist + args)

	for cell in var.cells
		new_cell = var_new.make_new_cell()
		new_cell.change_arg("onset", cell.onset)
		new_cell.change_arg("offset", cell.offset)
		for arg in var.arglist
			v = eval "cell.#{arg}"
			new_cell.change_arg(arg, v)
		end
	end

	return var_new
end



#-------------------------------------------------------------------
# Method name: create_mutually_exclusive
# Function: Create a new column from two others, mixing their cells together
#  such that the new variable has all of the arguments of both other variables
#  and a new cell for each overlap and mixture of the two cells.  Mixing two
#  variables together.
# Arguments:
# => name (required): The name of the new variable.
# => var1name (required): Name of the first variable to be mutexed.
# => var2name (required): Name of the second variable to be mutexed.
#
# Returns:
# => The new Ruby representation of the variable.  Write it back to the database
# to save it.
#
# Example:
# test = create_mutually_exclusive("test", "var1", "var2")
# setVariable("test",test)
# -------------------------------------------------------------------

def combine_columns(name, varnames)
	stationary_var = varnames[0]
	for i in 1...varnames.length
		next_var = varnames[i]
		var = create_mutually_exclusive(name, stationary_var, next_var)
	end
	return var
end

def scan_for_bad_cells(col)
	error = false
	for cell in col.cells
		if cell.onset > cell.offset
			puts "ERROR AT CELL " + cell.ordinal.to_s + " IN COLUMN " + col.name + ", the onset is > than the offset."
			error = true
		end
		if error
			puts "Please fix these errors, as the script cannot continue until then."
			exit
		end
	end
end

def get_later_overlapping_cell(col)
	col.sort_cells()
	overlapping_cells = Array.new
	for i in 0..col.cells.length - 2
		cell1 = col.cells[i]
		cell2 = col.cells[i+1]
		if (cell1.onset <= cell2.onset and cell1.offset >= cell2.onset)
			overlapping_cells << cell2
		end
	end
	return overlapping_cells
end

def fix_one_off_cells(col1, col2)
	for i in 0..col1.cells.length-2
		cell1 = col1.cells[i]
		for j in 0..col2.cells.length-2
			cell2 = col2.cells[j]

			if (cell1.onset - cell2.onset).abs == 1
				print_debug "UPDATING CELL"
				cell2.change_arg("onset", cell1.onset)
				print_debug "CELL2 ONSET IS NOW " + cell1.onset.to_s
				if j > 0 and col2.cells[j-1].offset == cell2.offset
					col2.cells[j-1].change_arg("offset", col2.cells[i-1].offset + 1)
				end
			end

			if (cell1.offset - cell2.offset).abs == 1
				print_debug "UPDATING CELL"
				cell2.change_arg("offset", cell1.offset)
				print_debug "CELL2 OFFSET IS NOW " + cell1.offset.to_s
				if col2.cells[j+1].onset == cell2.offset
					col2.cells[j+1].change_arg("onset", col2.cells[i-1].onset + 1)
				end
			end

			if cell2.onset - cell1.offset == 1
				print_debug "UPDATING CELL"
				cell1.change_arg("offset", cell2.onset)
				print_debug "CELL1 OFFSET IS NOW " + cell2.onset.to_s
				if col1.cells[i+1].onset == cell1.offset
					col1.cells[i+1].change_arg("onset", col1.cells[i+1].onset + 1)
				end
			end
			if cell1.onset - cell2.offset == 1
				print_debug "UPDATING CELL"
				cell2.change_arg("offset", cell1.onset)
				print_debug "CELL2 OFFSET IS NOW " + cell1.onset.to_s
				if col2.cells[j+1].onset == cell2.offset
					col2.cells[j+1].change_arg("onset", col2.cells[i+1].onset + 1)
				end
			end
		end
	end
end
def create_mutually_exclusive(name, var1name, var2name, var1_argprefix=nil, var2_argprefix=nil)
	return createMutuallyExclusive(name, var1name, var2name, var1_argprefix, var2_argprefix)
end
def createMutuallyExclusive(name, var1name, var2name, var1_argprefix=nil, var2_argprefix=nil)
	if var1name.class == "".class
		var1 = getVariable(var1name)
	else
		var1 = var1name
	end
	if var2name.class == "".class
		var2 = getVariable(var2name)
	else
		var2 = var2name
	end

	scan_for_bad_cells(var1)
	scan_for_bad_cells(var2)
	# fix_one_off_cells(var1, var2)

	# for i in 0..var1.cells.length-2
	#     cell1 = var1.cells[i]
	#     cell2 = var1.cells[i+1]
	#     if cell1.offset == cell2.onset
	#         puts "WARNING: Found cells with the same onset/offset.  Adjusting onset by 1."
	#         cell2.change_arg("onset", cell2.onset+1)
	#     end
	# end
	for cell in var1.cells
		if cell.offset == 0
			puts "ERROR: CELL IN " + var1.name + " ORD: " + cell.ordinal.to_s + "HAS BLANK OFFSET, EXITING"
			exit
		end
	end

	for cell in var2.cells
		if cell.offset == 0
			puts "ERROR: CELL IN " + var2.name + " ORD: " + cell.ordinal.to_s + "HAS BLANK OFFSET, EXITING"
			exit
		end
	end

	# for i in 0..var2.cells.length-2
	#     cell1 = var2.cells[i]
	#     cell2 = var2.cells[i+1]
	#     if cell1.offset == cell2.onset
	#         puts "WARNING: Found cells with the same onset/offset.  Adjusting onset by 1."
	#         # cell2.change_arg("onset", cell2.onset+1)
	#     end
	# end

	# Handle special cases where one or both of columns have no cells

	# Handle special case where column has a cell with negative time

	# Get the earliest time between the two cols
	time1_on = 9999999999
	time2_on = 9999999999

	time1_off = 0
	time2_off = 0
	if var1.cells.length > 0
		time1_on = var1.cells[0].onset
		time1_off = var1.cells[var1.cells.length-1].offset
	end
	if var2.cells.length > 0
		time2_on = var2.cells[0].onset
		time2_off = var2.cells[var2.cells.length-1].offset
	end
	start_time = [time1_on, time2_on].min

	# And the end time
	end_time = [time1_off, time2_off].max


	# Create the new variable
	if var1_argprefix == nil
		var1_argprefix = var1.name.gsub(/(\W)+/,"").downcase + "___"
		var1_argprefix.gsub(".", "")
	end
	if var2_argprefix == nil
		var2_argprefix = var2.name.gsub(/(\W)+/,"").downcase + "___"
		var2_argprefix.gsub(".","")
	end

	v1arglist = var1.arglist.map { |arg| var1_argprefix + arg }
	v2arglist = var2.arglist.map { |arg| var2_argprefix + arg }

	# puts "NEW ARGUMENT NAMES:", v1arglist, v2arglist
	args = Array.new
	args << (var1_argprefix + "ordinal")
	args += v1arglist

	args << (var2_argprefix + "ordinal")
	args += v2arglist

	# puts "Creating mutex var", var1.arglist
	mutex = createVariable(name, args)
	# puts "Mutex var created"

	# And finally begin creating new cells
	v1cell = nil
	v2cell = nil
	next_v1cell_ind = nil
	next_v2cell_ind = nil

	time = start_time
	# puts "Start time", start_time
	# puts "End time", end_time

	flag = false

	count = 0

	#######################
	# BEGIN NEW MUTEX
	# Idea here: gather all of the time changes.
	# For each time change get the corresponding cells involved in that change.
	# Create the necessary cell at each time change.
	#######################

	time_changes = Set.new
	v1_cells_at_time = Hash.new
	v2_cells_at_time = Hash.new


	# Preprocess relevant cells and times
	for cell in var1.cells + var2.cells
		time_changes.add(cell.onset)
		time_changes.add(cell.offset)
	end


	time_changes = time_changes.to_a.sort
	if $debug
		p time_changes
	end
	# p time_changes


	mutex_cell = nil
	mutex_cell_parent = nil

	# TODO: make these handle empty cols
	v1cell = var1.cells[0]
	prev_v1cell = nil
	prev_v2cell = nil
	v2cell = var2.cells[0]
	v1idx = 0
	v2idx = 0

	#
	for i in 0..time_changes.length-2
		t0 = time_changes[i]
		t1 = time_changes[i+1]

		# Find the cells that are active during these times
		for j in v1idx..var1.cells.length-1
			c = var1.cells[j]
			v1cell = nil
			if $debug
				p "---", "T1", t0, t1, c.onset, c.offset, "---"
			end
			if c.onset <= t0 and c.offset >= t1 and ( t1-t0 > 1 or (c.onset==t0 and c.offset==t1) )
				v1cell = c
				v1idx = j
				# p t0, t1, "Found V1"
				break
				# elsif c.onset > t1
				#   break
			else
				v1cell = nil
			end
		end

		for j in v2idx..var2.cells.length-1
			c = var2.cells[j]
			v2cell = nil
			# p "---", "T2", t0, t1, c.onset, c.offset, "---"
			if c.onset <= t0 and c.offset >= t1 and ( t1-t0 > 1 or (c.onset==t0 and c.offset==t1) )
				v2cell = c
				v2idx = j
				# p t0, t1, "Found V2"
				break
				# elsif c.onset > t1
				#   break
			else
				v2cell = nil
			end
		end

		if v1cell != nil or v2cell != nil
			mutex_cell = mutex.create_cell

			mutex_cell.change_arg("onset", t0)
			mutex_cell.change_arg("offset", t1)
			fillMutexCell(v1cell, v2cell, mutex_cell, mutex, var1_argprefix, var2_argprefix)
		end

	end


	# Now that we have all of the necessary temporal information
	# go through each time in the list and create a cell

	for arg in mutex.arglist
		mutex.change_arg_name(arg, arg.gsub("___", "_"))
	end
	for i in 0..mutex.cells.length-1
		c = mutex.cells[i]
		c.change_arg("ordinal", i+1)
	end
	puts "Created a column with ", mutex.cells.length, " cells."

	return mutex
end

def fillMutexCell(v1cell, v2cell, cell, mutex, var1_argprefix, var2_argprefix)
	if v1cell != nil and v2cell != nil
		for arg in mutex.arglist
			a = arg.gsub(var1_argprefix, "")
			if arg.index(var1_argprefix) == 0
				v = eval "v1cell.#{a}"
				cell.change_arg(arg, v)
			end

			a = arg.gsub(var2_argprefix, "")
			if arg.index(var2_argprefix) == 0
				v = eval "v2cell.#{a}"
				cell.change_arg(arg, v)
			end
		end

	elsif v1cell != nil and v2cell == nil
		for arg in mutex.arglist
			a = arg.gsub(var1_argprefix, "")
			if arg.index(var1_argprefix) == 0
				v = eval "v1cell.#{a}"
				cell.change_arg(arg, v)
			end
		end

	elsif v1cell == nil and v2cell != nil
		for arg in mutex.arglist
			a = arg.gsub(var2_argprefix, "")
			if arg.index(var2_argprefix) == 0
				v = eval "v2cell.#{a}"
				cell.change_arg(arg, v)
			end
		end
	end
end



#-------------------------------------------------------------------
# Method name: load_db
# Function: Loads a new database from a file.  DOES NOT ALTER THE GUI.
# Arguments:
# => filename (required): The FULL PATH to the saved Datavyu file.
#
# Returns:
# => db: The database of the opened project.  Set to $db to use other
#     functions with it.
# => pj: The project data of the opened project.  Set to $pj to use other
#     functions with it.
#
# Example:
# $db,$pj = load_db("/Users/username/Desktop/test.opf")
# -------------------------------------------------------------------
def load_db(filename)
	loadDB(filename)
end
def loadDB(filename)
	# Packages needed for opening and saving projects and databases.


	#
	# ****************************************************************************
	# *** Check to make sure filename below is the absolute path to a project. ***
	# ****************************************************************************
	#
	#
	# Main body of example script:
	#
	print_debug "Opening Project: "

	# Create the controller that holds all the logic for opening projects and
	# databases.
	open_c = OpenC.new

	#
	# Opens a project and associated database (i.e. either compressed or
	# uncompressed .shapa files). If you want to just open a standalone database
	# (i.e .odb or .csv file) call open_c.open_database("filename") instead. These
	# methods do *NOT* open the project within the Datavyu UI.
	#
	db = nil
	proj = nil
	if filename.include?(".csv")
		open_c.open_database(filename)
	else
		open_c.open_project(filename)
		# Get the project that was opened (if you want).
		proj = open_c.get_project
	end

	# Get the database that was opened.
	db = open_c.get_datastore


	# If the open went well - query the database, do calculations or whatever
	unless db.nil?
		# This just prints the number of columns in the database.
		print_debug "SUCCESSFULLY Opened a project with '" + db.get_all_variables.length.to_s + "' columns!"
	else
		print_debug "Unable to open the project '" + filename + "'"
	end

	print_debug filename + " has been loaded."

	return db, proj
end


#-------------------------------------------------------------------
# Method name: save_db
# Function: Saves the current $db and $pj variables to filename.  If
#     filename ends with .csv, it saves a .csv file.  Otherwise it saves
#     it as a .opf.
# Arguments:
# => filename (required): The FULL PATH to where the Datavyu file should
#        be saved.
#
# Returns:
# => Nothing.
#
# Example:
# save_db("/Users/username/Desktop/test.opf")
# -------------------------------------------------------------------
def save_db(filename)
	saveDB(filename)
end
def saveDB(filename)
	#
	# Main body of example script:
	#
	print_debug "Saving Database: " + filename

	# Create the controller that holds all the logic for opening projects and
	# databases.
	save_c = SaveC.new

	#
	# Saves a database (i.e. a .odb or .csv file). If you want to save a project
	# call save_project("project file", project, database) instead.
	# These methods do *NOT* alter the Datavyu UI.
	#
	if filename.include?('.csv')
		save_c.save_database(filename, $db)
	else
		#if $pj == nil or $pj.getDatabaseFileName == nil
		$pj = Project.new()
		$pj.setDatabaseFileName("db")
		dbname = filename[filename.rindex("/")+1..filename.length]
		$pj.setProjectName(dbname)
		#end
		save_file = java.io.File.new(filename)
		save_c.save_project(save_file, $pj, $db)
	end

	print_debug "Save successful."

end

# This method is deprecated, only kept for backwards compatibility
def delete_column(colname)
	deleteVariable(colname)
end

def deleteVariable(colname)
	if colname.class != "".class
		colname = colname.name
	end
	col = $db.getVariable(colname)
	if (col == nil)
		printNoColumnFoundWarning(colname.to_s)
	end
	$db.removeVariable(col)
end

# Let the user know that a given column was not found. Error is confusing, this should clarify.
def printNoColumnFoundWarning(colName)
	puts "WARNING: No column with name '" + colName + "' was found!"
end


#-------------------------------------------------------------------
# Method name: load_macshapa_db
# Function: Opens an old, closed database format MacSHAPA file and loads
#     it into the current open database.
#
#     WARNING: This will only read in
#     matrix and string variables.  Predicates are not yet supported.
#     Queries will not be read in.  Times are translated to milliseconds
#     for compatibility with Datavyu.
# Arguments:
# => filename (required): The FULL PATH to the saved MacSHAPA file.
# => write_to_gui (required): Whether the MacSHAPA file should be read into
#        the database currently open in the GUI or whether it should just be
#        read into the Ruby interface.  After this script is run $db and $pj
#        are now the MacSHAPA file.
#
# Returns:
# => db: The database of the opened project.
# => pj: The project data of the opened project.
#
# Example:
# $db,$pj = load_db("/Users/username/Desktop/test.opf")
# -------------------------------------------------------------------
def load_macshapa_db(filename, write_to_gui, *ignore_vars)
	loadMacshapaDB(filename, write_to_gui, ignore_vars)
end
def loadMacshapaDB(filename, write_to_gui, *ignore_vars)


	# Create a new DB for us to use so we don't touch the GUI... some of these
	# files can be huge.
	# Since I don't know how to make a whole new project, lets just load a blank file.
	if not write_to_gui
		#$db,$pj = load_db("/Users/j4lingeman/Desktop/blank.opf")
		# $db = Datastore.new
		# $pj = Project.new()
	end



	puts "Opening file"
	f = File.open(filename, 'r')

	puts "Opened file"
	# Read and split file by lines.  '\r' is used because that is the default
	# format for OS9 files.
	lines = ""
	while (line = f.gets)
		lines += line
	end
	lines = lines.split(/[\r\n]/)

	# Find the variable names in the file and use these to create and set up
	# our columns.
	predIndex = lines.index("***Predicates***")
	varIndex = lines.index("***Variables***")
	spreadIndex = lines.index("***SpreadPane***")
	predIndex += 2

	variables = Hash.new
	varIdent = Array.new

	while predIndex < varIndex
		l = lines[predIndex].split(/ /)[5]
		varname = l[0..l.index("(") - 1]
		if varname != "###QueryVar###" and varname != "div" and varname != "qnotes" \
			and not ignore_vars.include?(varname)
			print_debug varname
			variables[varname] = l[l.index("(")+1..l.length-2].split(/,/)
			varIdent << l
		end
		predIndex += 1
	end

	puts "Got predicate index"

	# Create the columns for the variables
	variables.each do |key, value|
		# Create column
		if getColumnList().include?(key)
			deleteVariable(key)
		end


		args = Array.new
		value.each { |v|
			# Strip out the ordinal, onset, and offset.  These will be handled on a
			# cell by cell basis.
			if v != "<ord>" and v != "<onset>" and v != "<offset>"
				args << v.sub("<", "").sub(">","")
			end
		}

		setVariable(createVariable(key, args))
	end

	# Search for where in the file the var's cells are, create them, then move
	# on to the next variable.
	varSection = lines[varIndex..spreadIndex]

	varIdent.each do |id|

		# Search the variable section for the above id
		varSection.each do |l|
			line = l.split(/[\t\s]/)
			if line[2] == id

				print_debug id
				col = getVariable(id.slice(0,id.index("(")))
				#print_debug varname
				start = varSection.index(l) + 1

				stringCol = false

				if varSection[start - 2].index("strID") != nil
					stringCol = true
				end

				#Found it!  Now build the cells
				while varSection[start] != "0"

					if stringCol == false
						cellData = varSection[start].split(/[\t]/)
						cellData[cellData.length - 1] = cellData[cellData.length-1][cellData[cellData.length-1].index("(")..cellData[cellData.length-1].length]
					else
						cellData = varSection[start].split(/[\t]/)
					end

					# Init cell to null

					cell = col.create_cell

					# Convert onset/offset from 60 ticks/sec to milliseconds
					onset = cellData[0].to_i / 60.0 * 1000
					offset = cellData[1].to_i / 60.0 * 1000

					# Set onset/offset of cell
					cell.change_arg("onset", onset.round)
					cell.change_arg("offset", offset.round)

					# Split up cell data
					data = cellData[cellData.length - 1]
					print_debug data
					if stringCol == false
						data = data[1..data.length-2]
						data = data.gsub(/[() ]*/, "")
						data = data.split(/,/)
					elsif data != nil #Then this is a string var
						data = data.strip()
						if data.split(" ").length > 1
							data = data[data.index(" ")..data.length] # Remove the char count
							data = data.gsub("/", " or ")
							data = data.gsub(/[^\w ]*/, "")
							data = data.gsub(/  /," ")
						else
							data = ""
						end
					else
						data = Array.new
						data << nil
					end
					# Cycle thru cell data arguments and fill them into the cell matrix
					narg = 0
					data.each_with_index do |d,i|
						print_debug cell.arglist[1]
						argname = cell.arglist[i]
						if d == nil
							cell.change_arg(argname, "")
						elsif d == "" or d.index("<") != nil
							cell.change_arg(argname,"")
						else
							cell.change_arg(argname,d)
						end
					end
					start += 1
				end
				setVariable(col)
			end
		end
	end

	f.close()

	return $db, $pj
end

#-------------------------------------------------------------------
# Method name: transfer_columns
# Function: Transfers columns between databases.  If db1 or db2 are set
#     to the empty string "", then that database is the current database
#     in $db (usually the GUI's database).  So if you want to transfer a
#     column into the GUI, set db2 to "".  If you want to tranfer a column
#     from the GUI into a file, set db1 to "".  Setting remove to true will
#     DELETE THE COLUMNS YOU ARE TRANSFERRING FROM DB1.  Be careful!
# Arguments:
# => db1 (required): The FULL PATH to the saved Datavyu file or set to
#     "" to use the currently opened database. Columns are transferred FROM here.
# => db2 (required): The FULL PATH to the saved Datavyu file or set to
#     "" to use the currently opened database.  Columns are tranferred TO here.
# => remove (required): Set to true to delete columns in DB1 as they are moved to
#     db2.  Set to false to leave them intact.
# => varnames (requires at least 1): You can specify as many var names as you like
#     that will be retrieved from db1.  These should be the string names of the
#     variables.
#
# Returns:
# => Nothing.  Saves the files in place or modifies the GUI
#
# Example:
#  transfer_columns("/Users/username/Desktop/test.opf","",true,"idchange")
#  The above example will transfer the column "idchange" from test.opf to the GUI
#  and leave test.opf intact with no modifications.
# -------------------------------------------------------------------
def transfer_columns(db1, db2, remove, *varnames)
	transferVariable(db1, db2, remove, *varnames)
end
def transferVariable(db1, db2, remove, *varnames)
	# Save the current $db and $proj global variables
	saved_db,saved_proj = $db,$proj

	# If varnames was specified as a hash, flatten it to an array
	varnames.flatten!

	# Display args when debugging
	print_debug("="*20)
	print_debug("#{__method__} called with following args:")
	print_debug(db1,db2,remove,varnames)
	print_debug("="*20)

	# Handle degenerate case of same source and destination
	if db1==db2
		puts "Warning: source and destination are identical.  No changes made."
		return nil
	end

	# Set the source database, loading from file if necessary.
	# Raises file not found error and returns nil if source database does not exist.
	db1path = ""
	begin
		if db1!=""
			db1path = File.expand_path(db1)
			if !File.readable?(db1path)
				raise "Error! File not readable : #{db1}"
			end
			print_debug("Loading source database from file : #{db1path}")
			from_db,from_proj = loadDB(db1path)
		else
			from_db,from_proj = $db,$proj
		end
	rescue StandardError => e
		puts e.message
		puts e.backtrace
		return nil
	end

	# Set the destination database, loading from file if necessary.
	# Raises file not found error and returns nil if destination database does not exist.
	db2path = ""
	begin
		if db2!=""
			db2path = File.expand_path(db2)
			if !File.writable?(db2path)
				raise "Error! File not writable : #{db2}"
			end
			print_debug("Loading destination database from file : #{db2path}")
			to_db,to_proj = loadDB(db2path)
			#$db,$proj = loadDB(db2path)
		else
			to_db,to_proj = $db,$proj
		end
	rescue StandardError => e
		puts e.message
		puts e.backtrace
		return nil
	end

	# Set working database to source database to prepare for reading
	$db,$pj = from_db,from_proj

	# Construct a hash to store columns and cells we are transferring
	print_debug("Fetching columns...")
	begin
		col_map = Hash.new
		cell_map = Hash.new
		for col in varnames
			c = getColumn(col.to_s)
			if c.nil?
				puts "Warning: column #{c} not found! Skipping..."
				next
			end
			col_map[col] = c
			cell_map[col] = c.cells
			print_debug("Read column : #{col.to_s}")
		end
	end

	# Set working database to destination database to prepare for writing
	$db,$pj = to_db,to_proj

	# Go through the hashmaps and reconstruct the columns
	begin
		for key in col_map.keys
			col = col_map[key]
			cells = cell_map[key]
			arglist = col.arglist

			# Construct a new variable and add all associated cells
			newvar = createVariable(key.to_s,arglist)
			for cell in cells
				c = newvar.make_new_cell()
				# Clone the existing cell arguments to the new cell.
				cell.arglist.each{ |x|
					c.change_arg(x,cell.get_arg(x))
				}
				c.ordinal = cell.ordinal
				c.onset = cell.onset
				c.offset = cell.offset
			end
			setVariable(key.to_s,newvar)
			print_debug("Wrote column : #{key.to_s} with #{newvar.cells.length} cells")
		end
	rescue StandardError => e
		puts "Failed trying to write column #{col}"
		puts e.message
		puts e.backtrace
		return nil
	end

	# Save the database to file if applicable
	saveDB(db2path) if db2path!=""

	# Final step: take care of deleting columns from source database if option is set.
	if remove
		$db,$pj = from_db,from_proj

		# Use our hashmap since it takes care of improper column names (returned nil from getColumn())
		col_map.keys.each{ |x|
			delete_column(x.to_s)
		}

		saveDB(db1path) if db1path!=""
	end

	# Restore the saved database and project globals
	$db,$proj = saved_db,saved_proj

	puts "Transfer completed successfully!"
end


#-------------------------------------------------------------------
# Method name: check_rel
# Function: Do a quick, in Datavyu, check of reliability errors.
# Arguments:
# => main_col (required): Either the string name or the Ruby column from getVariable
#     of the primary column to compare against.
# => rel_col (required): Either the string name or the Ruby column from getVariable
#     of the reliability column to compare to the primary column.
# => match_arg (required): The string of the argument to use to match the relability
#     cells to the primary cells.  This must be a unique identifier between the cells.
# => time_tolerance (required): The amount of slack you allow, in milliseconds, for
#     difference between onset and offset before it is considered an error.  Set to 0
#     for no difference allowed and to a very large number for infinite distance allowed.
# => dump_file (optional): The full string path to dump the relability output to.  This
#     can be used for multi-file dumps or just to keep a log.  You can also give it a Ruby
#     File object if a file is already started.
#
# Returns:
# => Nothing but the console and file output.
#
# Example:
#  check_rel("trial", "rel.trial", "trialnum", 100, "/Users/motoruser/Desktop/Relcheck.txt")
#   or
#  check_rel("trial", "rel.trial", "trialnum", 100)
# -------------------------------------------------------------------
def check_rel(main_col, rel_col, match_arg, time_tolerance, *dump_file)
	checkReliability(main_col, rel_col, match_arg, time_tolerance, dump_file[0])
end
def checkReliability(main_col, rel_col, match_arg, time_tolerance, *dump_file)
	# Make the match_arg conform to the method format that is used
	if ["0","1","2","3","4","5","6","7","8","9"].include?(match_arg[0].chr)
		match_arg = match_arg[1..match_arg.length]
	end
	match_arg = match_arg.gsub(/(\W)+/,"").downcase

	# Set up our method variables
	dump_file = dump_file[0]
	if main_col.class == "".class
		main_col = getVariable(main_col)
	end
	if rel_col.class == "".class
		rel_col = getVariable(rel_col)
	end

	printing = false
	if dump_file != nil
		if dump_file.class == "".class
			dump_file = open(dump_file,'a')
		end
		printing = true
	end

	# Define interal function for printing errors
	def print_err(m_cell, r_cell, arg, dump_file, main_col, rel_col)
		main_val = eval "m_cell.#{arg}"
		rel_val = eval "r_cell.#{arg}"
		err_str = "ERROR in " + main_col.name + " at Ordinal " + m_cell.ordinal.to_s + ", rel ordinal " + r_cell.ordinal.to_s + " in argument " + arg + ": " + main_val.to_s + ", " + rel_val.to_s + "\n"
		if dump_file != nil
			dump_file.write(err_str)
		end
		print err_str
	end

	# Build error array
	errors = Hash.new
	for arg in main_col.arglist
		errors[arg] = 0
	end
	errors["onset"] = 0
	errors["offset"] = 0

	# Now check the cells
	for mc in main_col.cells
		main_bind = eval "mc.#{match_arg}"
		for rc in rel_col.cells
			rel_bind = eval "rc.#{match_arg}"
			if main_bind == rel_bind
				# Then check these cells match, check them for errors
				if (mc.onset - rc.onset).abs >= time_tolerance
					print_err(mc, rc, "onset", dump_file, main_col, rel_col)
					errors["onset"] = errors["onset"] + 1
				end
				if (mc.offset - rc.offset).abs >= time_tolerance
					print_err(mc, rc, "offset", dump_file, main_col, rel_col)
					errors["offset"] = errors["offset"] + 1
				end

				for arg in main_col.arglist
					main_val = eval "mc.#{arg}"
					rel_val = eval "rc.#{arg}"
					if main_val != rel_val
						print_err(mc, rc, arg, dump_file, main_col, rel_col)
						errors[arg] = errors[arg] + 1
					end
				end
			end
		end
	end

	for arg, errs in errors
		str = "Total errors for " + arg + ": " + errs.to_s + ", Agreement:" + "%.2f" % (100 * (1.0 - (errs / rel_col.cells.length.to_f))) + "%\n"
		print str
		if dump_file != nil
			dump_file.write(str)
			dump_file.flush()
		end
	end

	return errors, rel_col.cells.length.to_f
end

#-------------------------------------------------------------------
# Method name: check_valid_codes
# Function: Do a quick, in Datavyu, check of valid codes.
# Arguments:
# => val (required): The variable that the codes belong to.
# => dump_file (required): The full path of the file to dump output to.
#     Use "" to not dump to a file.  You may also pass a Ruby File object.
# => arg_code_pairs (required): A list of the argument names and valid codes
#     in the following format: "argument_name", ["y","n"], "argument2", ["j","k","m"]
# Returns:
# => Nothing but the console and file output.
#
# Example:
#  check_valid_codes("trial", "", "hand", ["l","r","b","n"], "turn", ["l","r"], "unit", [1,2,3])
# -------------------------------------------------------------------
def check_valid_codes(var, dump_file, *arg_code_pairs)
	checkValidCodes(var, dump_file, *arg_code_pairs)
end
def checkValidCodes(var, dump_file, *arg_code_pairs)
	if var.class == "".class
		var = getVariable(var)
	end

	if dump_file != ""
		if dump_file.class == "".class
			dump_file = open(dump_file, 'a')
		end
	end

	# Make the argument/code hash
	arg_code = Hash.new
	for i in 0...arg_code_pairs.length
		if i % 2 == 0
			if arg_code_pairs[i].class != "".class
				print_debug 'FATAL ERROR in argument/valid code array.  Exiting.  Please check to make sure it is in the format "argumentname", ["valid","codes"]'
				exit
			end
			arg = arg_code_pairs[i]
			if ["0","1","2","3","4","5","6","7","8","9"].include?(arg[1].chr)
				arg = arg[1..arg.length]
			end
			arg = arg.gsub(/(\W )+/,"").downcase

			arg_code[arg] = arg_code_pairs[i+1]
		end
	end

	errors = false
	for cell in var.cells
		for arg, code in arg_code
			val = eval "cell.#{arg}"
			if not code.include?(val)
				errors = true
				str = "Code ERROR: Var: " + var.name + "\tOrdinal: " + cell.ordinal.to_s + "\tArg: " + arg + "\tVal: " + val + "\n"
				print str
				if dump_file != ""
					dump_file.write(str)
				end
			end
		end
	end
	if not errors
		print_debug "No errors found."
	end
end

def getColumnList()
	return getVariableList()
end
def getVariableList()
	name_list = Array.new
	vars = $db.getAllVariables()
	for v in vars
		name_list << v.name
	end

	return name_list
end

def printAllNested(file)
	columns = getColumnList()
	columns.sort! # This is just so everything is the same across runs, regardless of column order
	# Scan each column, getting a list of how many cells the cells of that
	# contain and how much time the cells of that column fill

	times = Hash.new

	for outer_col in columns
		collected_time = 0
		for cell in outer_col.cells
			collected_time += cell.offset - cell.onset
		end
		times[outer_col.name] = collected_time
	end

	# Now, we want to loop over the columns in the order of the amount of data
	# that they take up.

end

def printCell(cell, file)

end

def smoothColumn(colname, tol=33)
	col = getVariable(colname)
	for i in 0..col.cells.length-2
		curcell = col.cells[i]
		nextcell = col.cells[i+1]

		if nextcell.onset - curcell.offset < tol
			nextcell.change_arg("onset", curcell.offset)
		end
	end
	setVariable(colname, col)
end

def print_codes(cell, file, args)
	print_args(cell, file, args)
end
def print_args(cell, file, args)
	for a in args
		#puts "Printing: " + a
		val = eval "cell.#{a}"
		file.write(val.to_s + "\t")
	end
end

def getCellFromTime(col, time)
	for cell in col.cells
		if cell.onset <= time and cell.offset >= time
			return cell
		end
	end
	return nil
end

def printCellCodes(cell)
	printCellArgs(cell)
end
def printCellArgs(cell)
	s = Array.new
	s << cell.ordinal.to_s
	s << cell.onset.to_s
	s << cell.offset.to_s
	for arg in cell.arglist
		s << cell.get_arg(arg)
	end
	return s
end

def deleteCell(cell)
	cell.db_cell.getVariable.removeCell(cell.db_cell)
end
